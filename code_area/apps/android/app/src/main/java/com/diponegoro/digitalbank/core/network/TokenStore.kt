package com.diponegoro.digitalbank.core.network

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Penyimpanan token sesi & token biometrik (DAFTAR_PEKERJAAN 5.4, NFR-06).
 * Nilai dienkripsi AES-GCM dengan kunci di Android Keystore sehingga tidak terbaca walau berkas
 * SharedPreferences disalin. Bila Keystore tidak tersedia (uji JVM), dipakai penyimpanan memori.
 */
interface TokenStore {
    data class Session(val accessToken: String, val refreshToken: String, val sessionId: String, val userId: String)

    fun session(): Session?
    fun save(session: Session)
    fun clearSession()

    /** Token perangkat untuk login biometrik (FR-03), terikat ke [deviceId]. */
    fun biometricToken(): String?
    fun saveBiometricToken(token: String?)

    /** ID perangkat stabil untuk device binding sesi. */
    fun deviceId(): String
}

class InMemoryTokenStore : TokenStore {
    @Volatile private var session: TokenStore.Session? = null
    @Volatile private var biometric: String? = null
    private val device = "device-" + UUID.randomUUID()
    override fun session() = session
    override fun save(session: TokenStore.Session) { this.session = session }
    override fun clearSession() { session = null }
    override fun biometricToken() = biometric
    override fun saveBiometricToken(token: String?) { biometric = token }
    override fun deviceId() = device
}

class SecureTokenStore(context: Context) : TokenStore {
    private val prefs = context.getSharedPreferences("ddb_secure", Context.MODE_PRIVATE)
    @Volatile private var cached: TokenStore.Session? = null

    override fun session(): TokenStore.Session? = cached ?: run {
        val a = read("access"); val r = read("refresh"); val s = read("sid"); val u = read("uid")
        if (a == null || r == null || s == null || u == null) null else TokenStore.Session(a, r, s, u).also { cached = it }
    }

    override fun save(session: TokenStore.Session) {
        cached = session
        write("access", session.accessToken); write("refresh", session.refreshToken)
        write("sid", session.sessionId); write("uid", session.userId)
    }

    override fun clearSession() {
        cached = null
        prefs.edit().remove("access").remove("refresh").remove("sid").remove("uid").apply()
    }

    override fun biometricToken(): String? = read("bio")
    override fun saveBiometricToken(token: String?) = if (token == null) prefs.edit().remove("bio").apply() else write("bio", token)

    override fun deviceId(): String = prefs.getString("device_id", null)
        ?: ("android-" + UUID.randomUUID()).also { prefs.edit().putString("device_id", it).apply() }

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return gen.generateKey()
    }

    private fun write(name: String, value: String) {
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val out = c.iv + c.doFinal(value.toByteArray(Charsets.UTF_8))
        prefs.edit().putString(name, Base64.encodeToString(out, Base64.NO_WRAP)).apply()
    }

    private fun read(name: String): String? = runCatching {
        val raw = Base64.decode(prefs.getString(name, null) ?: return null, Base64.NO_WRAP)
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, raw.copyOfRange(0, 12)))
        String(c.doFinal(raw.copyOfRange(12, raw.size)), Charsets.UTF_8)
    }.getOrNull()

    companion object {
        private const val ALIAS = "ddb_token_key"

        /** Keystore tersedia di perangkat nyata; di Robolectric/JVM jatuh ke memori. */
        fun create(context: Context): TokenStore = runCatching {
            KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            SecureTokenStore(context)
        }.getOrElse { InMemoryTokenStore() }
    }
}
