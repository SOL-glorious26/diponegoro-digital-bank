package com.diponegoro.digitalbank.core.network

import com.diponegoro.digitalbank.core.util.AppResult
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Klien REST ke backend bersama (DAFTAR_PEKERJAAN 5.1, SAD-DDB-001 Bab VI).
 *  - Menyisipkan "Authorization: Bearer <accessToken>" pada setiap permintaan.
 *  - Saat 401, menukar refresh token (POST /auth/refresh) satu kali lalu mengulang permintaan;
 *    bila gagal, sesi dihapus dan [onSessionExpired] dipanggil agar UI kembali ke layar login.
 */
class ApiClient(
    baseUrl: String,
    private val tokens: TokenStore,
    private val onSessionExpired: () -> Unit = {},
    client: OkHttpClient? = null
) {
    val json = Json { ignoreUnknownKeys = true; explicitNulls = false; coerceInputValues = true }

    private val refreshApi: DdbApi = retrofit(baseUrl, (client ?: OkHttpClient()).newBuilder().build())

    val api: DdbApi = retrofit(
        baseUrl,
        (client ?: OkHttpClient()).newBuilder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(BearerInterceptor())
            .authenticator(RefreshAuthenticator())
            .build()
    )

    private fun retrofit(baseUrl: String, http: OkHttpClient): DdbApi = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(http)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(DdbApi::class.java)

    /** Menyimpan token hasil login/registrasi/refresh. */
    fun saveSession(t: AuthTokens) {
        tokens.save(TokenStore.Session(t.accessToken, t.refreshToken, t.sessionId, t.user.id))
    }

    private inner class BearerInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val access = tokens.session()?.accessToken
            val req = chain.request()
            if (access == null || req.header("Authorization") != null || isPublic(req)) return chain.proceed(req)
            return chain.proceed(req.newBuilder().header("Authorization", "Bearer $access").build())
        }
    }

    private inner class RefreshAuthenticator : Authenticator {
        override fun authenticate(route: Route?, response: Response): Request? {
            if (isPublic(response.request) || response.priorResponse != null) return null
            synchronized(this) {
                val current = tokens.session() ?: return null
                val sent = response.request.header("Authorization")?.removePrefix("Bearer ")
                // Permintaan lain mungkin sudah me-refresh token lebih dulu.
                if (sent != null && sent != current.accessToken) {
                    return response.request.newBuilder().header("Authorization", "Bearer ${current.accessToken}").build()
                }
                val renewed = runCatching {
                    kotlinx.coroutines.runBlocking { refreshApi.refresh(RefreshRequest(current.refreshToken)) }
                }.getOrNull()
                if (renewed == null) {
                    tokens.clearSession()
                    onSessionExpired()
                    return null
                }
                saveSession(renewed)
                return response.request.newBuilder().header("Authorization", "Bearer ${renewed.accessToken}").build()
            }
        }
    }

    private fun isPublic(req: Request): Boolean {
        val p = req.url.encodedPath
        return PUBLIC.any { p.endsWith(it) } || p.contains("/api/v1/auth/recovery/")
    }

    /** Ubah panggilan API menjadi AppResult dengan pesan siap tampil (field "message" dari backend). */
    suspend fun <T> call(block: suspend DdbApi.() -> T): AppResult<T> = try {
        AppResult.Success(api.block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        AppResult.Failure(errorMessage(e))
    } catch (e: IOException) {
        log(e)
        AppResult.Failure(OFFLINE)
    } catch (e: Exception) {
        log(e)
        AppResult.Failure("Terjadi kesalahan: ${e.message ?: e::class.simpleName}")
    }

    /** Log diagnosis tanpa data sensitif (hanya jenis & pesan exception). Diam di uji JVM. */
    private fun log(e: Exception) {
        runCatching { android.util.Log.w("DdbApi", "${e::class.java.simpleName}: ${e.message}") }
    }

    fun errorMessage(e: HttpException): String {
        val body = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
        val parsed = body?.let { runCatching { json.decodeFromString(ErrorBody.serializer(), it) }.getOrNull() }
        return parsed?.message?.takeIf { it.isNotBlank() }
            ?: parsed?.fields?.values?.firstOrNull()
            ?: when (e.code()) {
                401 -> "Sesi berakhir. Silakan login kembali."
                in 500..599 -> "Server sedang bermasalah. Coba beberapa saat lagi."
                else -> "Permintaan gagal (HTTP ${e.code()})"
            }
    }

    /** Kode error stabil dari backend (mis. PIN_INVALID), untuk logika klien. */
    fun errorCode(e: HttpException): String? {
        val body = runCatching { e.response()?.errorBody()?.string() }.getOrNull() ?: return null
        return runCatching { json.decodeFromString(ErrorBody.serializer(), body).code }.getOrNull()
    }

    companion object {
        const val OFFLINE = "Tidak dapat terhubung ke server. Periksa koneksi internet Anda."
        private val PUBLIC = listOf(
            "/api/v1/auth/register", "/api/v1/auth/register/verify-otp", "/api/v1/auth/register/resend-otp",
            "/api/v1/auth/login", "/api/v1/auth/biometric-login", "/api/v1/auth/refresh"
        )
    }
}

/** Konversi waktu backend. java.time tidak dipakai karena minSdk 24. */
object IsoTime {
    private val WIB: TimeZone = TimeZone.getTimeZone("Asia/Jakarta")

    /** "2026-11-11T05:12:34.123456Z" atau "...+07:00" → epoch millis. */
    fun parse(iso: String?): Long {
        if (iso.isNullOrBlank()) return 0L
        val m = Regex("""^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2})(?:\.(\d+))?(Z|[+-]\d{2}:?\d{2})?$""").find(iso.trim())
            ?: return 0L
        val (base, frac, zone) = m.destructured
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone(if (zone.isEmpty() || zone == "Z") "UTC" else "GMT" + zone.let { if (it.contains(':')) it else it.substring(0, 3) + ":" + it.substring(3) })
        }
        val millis = frac.padEnd(3, '0').take(3).toIntOrNull() ?: 0
        return (fmt.parse(base)?.time ?: 0L) + millis
    }

    /** "2026-12-01" (tanggal kalender WIB) → epoch millis awal hari. */
    fun parseDate(date: String?): Long {
        if (date.isNullOrBlank()) return 0L
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = WIB }.parse(date)?.time ?: 0L
    }

    /** Epoch millis → "yyyy-MM-dd" (tanggal kalender WIB). */
    fun formatDate(millis: Long): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = WIB }.format(millis)
}
