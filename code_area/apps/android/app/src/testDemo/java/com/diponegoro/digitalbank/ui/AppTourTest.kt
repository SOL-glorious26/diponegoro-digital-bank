package com.diponegoro.digitalbank.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.diponegoro.digitalbank.MainActivity
import com.diponegoro.digitalbank.TestEnv
import com.diponegoro.digitalbank.core.ui.PinDialog
import com.diponegoro.digitalbank.core.ui.ReceiptCard
import com.diponegoro.digitalbank.ok
import com.diponegoro.digitalbank.topup.data.LocalTopUpRepository
import com.diponegoro.digitalbank.transfer.data.LocalTransferRepository
import com.diponegoro.digitalbank.ui.theme.DigitalBankTheme
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream
import java.time.Duration

private val outDir = File("build/screenshots").apply { mkdirs() }

private fun AndroidComposeTestRule<*, *>.pump(ms: Long = 200) {
    mainClock.autoAdvance = false
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))
    mainClock.advanceTimeBy(ms)
    waitForIdle()
}

/** Menggambar semua window (layar + dialog) ke satu PNG, seperti tampilan di perangkat. */
@Suppress("UNCHECKED_CAST")
private fun AndroidComposeTestRule<*, *>.shot(name: String) {
    pump(400)
    val global = Class.forName("android.view.WindowManagerGlobal").getMethod("getInstance").invoke(null)
    val views = (global.javaClass.getDeclaredField("mViews").apply { isAccessible = true }.get(global) as List<View>)
        .filter { it.isShown && it.width > 0 }
    val base = views.first()
    val image = Bitmap.createBitmap(base.width, base.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(image)
    views.forEachIndexed { index, view ->
        if (index > 0) canvas.drawColor(android.graphics.Color.argb(110, 0, 0, 0))
        val location = IntArray(2).also { view.getLocationOnScreen(it) }
        canvas.save()
        canvas.translate(location[0].toFloat(), location[1].toFloat())
        view.draw(canvas)
        canvas.restore()
    }
    FileOutputStream(File(outDir, "$name.png")).use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
}

/**
 * Uji UI end-to-end di JVM: login akun demo, menavigasi layar setiap modul seperti pengguna,
 * memeriksa pesan validasi, dan menyimpan screenshot ke app/build/screenshots.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "xxhdpi") // qualifier ukuran layar membuat dialog macet di Robolectric
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppTourTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun waitForText(text: String, substring: Boolean = false, timeoutMs: Long = 8_000) {
        var waited = 0L
        while (rule.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isEmpty()) {
            if (waited > timeoutMs) {
                rule.shot("GAGAL_${text.take(20).replace(Regex("[^A-Za-z0-9]"), "_")}")
                throw AssertionError("Teks tidak muncul: $text")
            }
            rule.pump(); waited += 200
        }
    }

    /**
     * Field terbaru dengan label tsb. Selama transisi NavHost, layar lama dan baru sama-sama
     * ada di komposisi; node yang terakhir dibuat adalah milik layar tujuan.
     */
    private fun field(label: String): SemanticsNodeInteraction =
        rule.onAllNodes(hasSetTextAction() and hasText(label, substring = true)).onLast()

    private fun type(label: String, value: String) {
        field(label).performSemanticsAction(SemanticsActions.SetText) { it(AnnotatedString(value)) }
        rule.pump()
    }

    /** Durasi transisi bawaan NavHost 700 ms; jam Compose tidak maju otomatis di [pump]. */
    private val transitionMs = 1_000L

    /**
     * Klik lewat aksi semantics, tidak perlu node terlihat di layar. Sengaja tanpa
     * performScrollTo: dengan mainClock.autoAdvance = false (lihat [pump]) scroll tidak pernah
     * selesai sehingga performScrollTo berputar berulang-ulang (login ±4,5 menit, Riwayat macet).
     */
    private fun click(text: String, substring: Boolean = false) {
        rule.onAllNodesWithText(text, substring = substring).onLast()
            .performSemanticsAction(SemanticsActions.OnClick)
        rule.pump(transitionMs)
    }

    private fun back() {
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        rule.pump(transitionMs)
    }

    @Test
    fun screensTour() {
        // Authentication
        waitForText("Masuk dengan biometrik")
        rule.shot("01_login")
        type("Username, email", "demo")
        type("Password", "Demo@1234")
        click("Masuk")
        waitForText("Masukkan PIN")
        rule.shot("01b_pin")
        "123456".forEach { click(it.toString()) } // keypad: PIN terkirim otomatis pada digit keenam
        waitForText("Saldo rekening utama")
        rule.shot("02_beranda")

        // FR-06: sembunyikan saldo
        rule.onNodeWithContentDescription("Sembunyikan saldo").performSemanticsAction(SemanticsActions.OnClick)
        rule.shot("03_beranda_saldo_disembunyikan")
        rule.onNodeWithContentDescription("Tampilkan saldo").performSemanticsAction(SemanticsActions.OnClick)
        rule.pump()

        // Transfer (UC-01 langkah 1–4 + alur alternatif A2, A3)
        click("Transfer")
        rule.shot("04_transfer_penerima")
        type("Nomor rekening", "1111111111")
        click("Cek rekening")
        waitForText("tidak valid", substring = true)
        rule.shot("05_transfer_rekening_tidak_valid")
        back(); click("Transfer")
        type("Nomor rekening", "1400000002")
        click("Cek rekening")
        waitForText("ANDI PRATAMA")
        type("Nominal transfer", "4000")
        click("Lanjut")
        waitForText("Nominal minimal", substring = true)
        rule.shot("06_transfer_nominal_minimal")
        type("Nominal transfer", "40000")
        type("Catatan", "Patungan makan")
        click("Lanjut")
        waitForText("Periksa kembali")
        rule.shot("07_transfer_konfirmasi")
        back(); back(); back()
        waitForText("Saldo rekening utama")

        // Riwayat & analitik
        click("Riwayat")
        waitForText("Riwayat Transaksi")
        rule.shot("08_riwayat")
        click("Filter jenis & status")
        click("Top-up & Tagihan")
        rule.shot("09_riwayat_filter")
        click("Analitik")
        waitForText("6 bulan terakhir")
        rule.shot("10_analitik")
        back()

        // QRIS
        click("QRIS")
        waitForText("Pindai kode QRIS")
        rule.shot("11_qris_pindai")
        click("Kopi Senja Tembalang", substring = true)
        waitForText("Detail pembayaran")
        rule.shot("12_qris_dinamis_konfirmasi")
        click("Batal")
        click("Terima dana dengan QRIS saya", substring = true)
        waitForText("QRIS pribadi (statis)")
        rule.shot("13_qris_terima")
        back()

        // Profil
        click("Profil")
        waitForText("Data pribadi")
        rule.shot("14_profil")
        click("Keamanan (password, PIN, biometrik)")
        waitForText("Ubah PIN transaksi")
        rule.shot("15_keamanan")
        back()
        click("Pengaturan notifikasi")
        waitForText("Kanal pengiriman")
        rule.shot("16_pengaturan_notifikasi")
        back()

        // Menu beranda
        click("Beranda")
        click("Grafik Saldo")
        waitForText("Saldo rekening utama")
        rule.shot("17_grafik_saldo")
        click("Bulanan")
        rule.shot("18_grafik_saldo_bulanan")
        back()

        click("Kantong")
        waitForText("Kantong Simpanan")
        rule.shot("19_kantong")
        back()

        click("Top-up")
        waitForText("Beli pulsa")
        type("Nomor HP", "081234567890")
        rule.shot("20_topup_pulsa")
        click("Token Listrik")
        type("ID pelanggan", "12345678901")
        click("Cek ID pelanggan")
        waitForText("Tarif/Daya")
        rule.shot("21_topup_pln")
        click("Rp100.000")
        waitForText("Konfirmasi Token Listrik")
        rule.shot("22_topup_konfirmasi")
        back()

        click("Kartu")
        waitForText("+ Buat kartu virtual")
        rule.shot("23_virtual_card")
        back()

        rule.onNodeWithContentDescription("Notifikasi", substring = true).performSemanticsAction(SemanticsActions.OnClick)
        rule.pump(transitionMs)
        waitForText("Dalam aplikasi", substring = true)
        rule.shot("24_notifikasi")
    }
}

/** Tampilan mode gelap: login, PIN, dan Beranda dengan qualifier "night". */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "night-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DarkModeShotsTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun waitForText(text: String) {
        var waited = 0L
        while (rule.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty()) {
            if (waited > 8_000) throw AssertionError("Teks tidak muncul: $text")
            rule.pump(); waited += 200
        }
    }

    private fun click(text: String) {
        rule.onAllNodesWithText(text).onLast().performSemanticsAction(SemanticsActions.OnClick)
        rule.pump(1_000)
    }

    private fun type(label: String, value: String) {
        rule.onAllNodes(hasSetTextAction() and hasText(label, substring = true)).onLast()
            .performSemanticsAction(SemanticsActions.SetText) { it(AnnotatedString(value)) }
        rule.pump()
    }

    @Test
    fun loginPinHomeDark() {
        waitForText("Masuk")
        rule.shot("40_gelap_login")
        type("Username, email", "demo")
        type("Password", "Demo@1234")
        click("Masuk")
        waitForText("Masukkan PIN")
        "12".forEach { click(it.toString()) }
        rule.shot("41_gelap_pin")
        "3456".forEach { click(it.toString()) }
        waitForText("Saldo rekening utama")
        rule.shot("42_gelap_beranda")
    }
}

/** Screenshot dialog PIN dan bukti transaksi dari transaksi nyata di ledger. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "xxhdpi") // qualifier ukuran layar membuat dialog macet di Robolectric
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReceiptAndDialogShotsTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val env = TestEnv()

    private fun render(content: @Composable () -> Unit) = rule.setContent {
        DigitalBankTheme {
            Surface(color = MaterialTheme.colorScheme.background) {
                Column(Modifier.padding(16.dp)) { content() }
            }
        }
    }

    @Test
    fun pinDialog() {
        render {
            PinDialog(
                subtitle = "Transfer Rp150.000 ke ANDI PRATAMA",
                errorMessage = "PIN salah. Sisa percobaan: 2",
                isLoading = false, onSubmit = {}, onDismiss = {}
            )
        }
        rule.shot("30_dialog_pin")
    }

    @Test
    fun transferReceipt() {
        val repo = LocalTransferRepository(env.ledger, env.guard)
        val tx = runBlocking {
            val r = repo.inquire(env.user, "BCA", "1234567890").ok()
            repo.execute(env.user, repo.quote(env.user, r, "150000", "Bayar kos").ok(), env.pin, "shot1").ok()
        }
        render { ReceiptCard(tx, "Transfer berhasil") }
        rule.shot("31_bukti_transfer")
    }

    @Test
    fun rejectedTransferReceipt() {
        val repo = LocalTransferRepository(env.ledger, env.guard)
        val tx = runBlocking {
            val r = repo.inquire(env.user, "BRI", "9999999999").ok()
            repo.execute(env.user, repo.quote(env.user, r, "100000", "").ok(), env.pin, "shot2").ok()
        }
        render { ReceiptCard(tx, "Transfer gagal — dana dikembalikan") }
        rule.shot("32_bukti_transfer_ditolak")
    }

    @Test
    fun plnReceipt() {
        val repo = LocalTopUpRepository(env.ledger, env.guard)
        val tx = runBlocking {
            val order = repo.orderPln(repo.inquirePln("12345678901").ok(), 100_000).ok()
            repo.execute(env.user, order, env.pin, "shot3").ok()
        }
        render { ReceiptCard(tx, "Transaksi berhasil") }
        rule.shot("33_bukti_token_listrik")
    }
}
