package com.diponegoro.digitalbank.core.network

import com.diponegoro.digitalbank.account.data.RemoteAccountRepository
import com.diponegoro.digitalbank.core.ledger.Direction
import com.diponegoro.digitalbank.core.ledger.TxStatus
import com.diponegoro.digitalbank.core.util.AppResult
import com.diponegoro.digitalbank.transfer.data.RemoteTransferRepository
import com.diponegoro.digitalbank.transfer.domain.Bank
import com.diponegoro.digitalbank.transfer.domain.Recipient
import com.diponegoro.digitalbank.transfer.domain.TransferQuote
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** DAFTAR_PEKERJAAN 5.6: uji lapisan REST dengan MockWebServer (tanpa backend sungguhan). */
class ApiClientTest {
    private lateinit var server: MockWebServer
    private val tokens = InMemoryTokenStore()
    private var expired = 0
    private lateinit var client: ApiClient

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start() }
        tokens.save(TokenStore.Session("old-access", "refresh-1", "sid-1", "user-1"))
        client = ApiClient(server.url("/").toString(), tokens, onSessionExpired = { expired++ })
    }

    @After
    fun tearDown() = server.shutdown()

    private fun json(code: Int, body: String) = MockResponse().setResponseCode(code)
        .setHeader("Content-Type", "application/json").setBody(body)

    private val overview = """{"ownerName":"DEMO","accountNumber":"1400123456","balance":250000,"pocketsTotal":0,"balanceHidden":false,"financialEnabled":true}"""
    private fun tokensJson(access: String, refresh: String) =
        """{"accessToken":"$access","refreshToken":"$refresh","sessionId":"sid-1","user":{"id":"user-1","username":"demo","status":"FINANCIAL_ENABLED","hasPin":true}}"""

    @Test
    fun `menyisipkan bearer token`() = runBlocking<Unit> {
        server.enqueue(json(200, overview))
        val r = RemoteAccountRepository(client).overview("user-1")
        assertEquals(250_000L, (r as AppResult.Success).data.balance)
        assertEquals("Bearer old-access", server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `401 memicu refresh token sekali lalu mengulang permintaan`() = runBlocking<Unit> {
        server.enqueue(json(401, """{"code":"UNAUTHORIZED","message":"Sesi tidak valid"}"""))
        server.enqueue(json(200, tokensJson("new-access", "refresh-2")))
        server.enqueue(json(200, overview))
        val r = RemoteAccountRepository(client).overview("user-1")
        assertTrue(r is AppResult.Success)
        server.takeRequest() // permintaan pertama (401)
        val refresh = server.takeRequest()
        assertEquals("/api/v1/auth/refresh", refresh.path)
        assertTrue(refresh.body.readUtf8().contains("refresh-1"))
        assertEquals("Bearer new-access", server.takeRequest().getHeader("Authorization"))
        assertEquals("refresh-2", tokens.session()?.refreshToken)
        assertEquals(0, expired)
    }

    @Test
    fun `refresh ditolak menghapus sesi dan memberi sinyal login ulang`() = runBlocking<Unit> {
        server.enqueue(json(401, "{}"))
        server.enqueue(json(401, """{"code":"UNAUTHORIZED","message":"Refresh token tidak valid"}"""))
        val r = RemoteAccountRepository(client).overview("user-1")
        assertTrue(r is AppResult.Failure)
        assertNull(tokens.session())
        assertEquals(1, expired)
    }

    @Test
    fun `pesan error backend diteruskan ke pengguna`() = runBlocking<Unit> {
        server.enqueue(json(422, """{"code":"INSUFFICIENT_BALANCE","message":"Saldo tidak mencukupi"}"""))
        val r = client.call { account() }
        assertEquals("Saldo tidak mencukupi", (r as AppResult.Failure).message)
    }

    @Test
    fun `server tidak terjangkau memberi pesan offline`() = runBlocking<Unit> {
        server.shutdown()
        val r = client.call { account() }
        assertEquals(ApiClient.OFFLINE, (r as AppResult.Failure).message)
    }

    @Test
    fun `transfer mengirim idempotency key dan memetakan transaksi`() = runBlocking<Unit> {
        server.enqueue(json(200, """{"id":"trx-1","reference":"DDB26111100AB12CD","category":"TRANSFER","status":"PROCESSING",
            "direction":"OUT","amount":100000,"fee":2500,"total":102500,"title":"Transfer antarbank","fromName":"DEMO",
            "fromDetail":"DDB • 1400123456","toName":"BUDI","toDetail":"BCA • 1234567890","note":"kos",
            "createdAt":"2026-11-11T05:12:34.123456Z","updatedAt":"2026-11-11T05:12:34Z"}"""))
        var changed = 0
        val repo = RemoteTransferRepository(client) { changed++ }
        val quote = TransferQuote(Recipient(Bank("BCA", "BCA"), "1234567890", "BUDI"), 100_000, 2_500, "kos", 1_000_000)
        val tx = (repo.execute("user-1", quote, "123456", "6f1c2a9e-0b7d-4c1e-9a33-1f2e3d4c5b6a") as AppResult.Success).data
        val req = server.takeRequest()
        assertEquals("6f1c2a9e-0b7d-4c1e-9a33-1f2e3d4c5b6a", req.getHeader("Idempotency-Key"))
        assertTrue(req.body.readUtf8().contains("\"pin\":\"123456\""))
        assertEquals(TxStatus.PROCESSING, tx.status)
        assertEquals(Direction.OUT, tx.directionFor("user-1"))
        assertEquals(102_500L, tx.total)
        assertEquals(IsoTime.parse("2026-11-11T05:12:34Z") + 123, tx.createdAt)
        assertEquals(1, changed)
    }

    @Test
    fun `kunci idempotensi dibersihkan sesuai aturan backend`() {
        assertEquals("abc-123:x.y", idemKey("abc-123:x.y"))
        assertEquals("abc", idemKey("a b/c"))
        assertEquals(64, idemKey("x".repeat(100)).length)
    }

    @Test
    fun `waktu ISO dan tanggal WIB`() {
        assertEquals(1_792_995_200_000L, IsoTime.parse("2026-10-26T06:13:20Z"))
        assertEquals(IsoTime.parse("2026-10-26T06:13:20Z"), IsoTime.parse("2026-10-26T13:13:20+07:00"))
        assertEquals("2026-12-01", IsoTime.formatDate(IsoTime.parseDate("2026-12-01")))
    }
}
