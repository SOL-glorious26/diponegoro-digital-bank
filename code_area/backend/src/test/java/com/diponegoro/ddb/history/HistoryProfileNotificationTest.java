package com.diponegoro.ddb.history;

import com.diponegoro.ddb.support.ApiTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HistoryProfileNotificationTest extends ApiTestBase {

    private Res transfer(Customer from, String bank, String number, long amount, String key) throws Exception {
        return pay("/api/v1/transfers", from.token(), key,
                Map.of("bankCode", bank, "accountNumber", number, "amount", amount, "note", "catatan " + key, "pin", PIN));
    }

    /** Notifikasi dibuat asinkron setelah commit; tunggu hingga muncul. */
    private Res waitNotifications(Customer c, int atLeast) throws Exception {
        Res r = null;
        for (int i = 0; i < 40; i++) {
            r = get("/api/v1/me/notifications", c.token());
            if (r.at("items").size() >= atLeast) return r;
            Thread.sleep(50);
        }
        return r;
    }

    // ================================================================== History

    @Test
    void riwayatFilterKombinasiPencarianDanPaginasi() throws Exception {
        Customer a = newCustomer();
        Customer b = newCustomer();
        transfer(a, "DDB", b.accountNumber(), 10_000, "h1");
        transfer(a, "BCA", "1234567890", 20_000, "h2");
        transfer(a, "BRI", "9999999999", 30_000, "h3");                       // gagal
        pay("/api/v1/qris/simulate-incoming", a.token(), "h4", Map.of("amount", 5_000, "payerName", "Budi"));

        Res all = get("/api/v1/history", a.token());
        assertThat(all.num("totalItems")).isEqualTo(5);                       // + setoran awal
        assertThat(all.at("items").get(0).get("title").asString()).isEqualTo("Terima dana QRIS"); // terbaru dulu

        assertThat(get("/api/v1/history?category=TRANSFER&status=FAILED", a.token()).num("totalItems")).isEqualTo(1);
        assertThat(get("/api/v1/history?direction=IN", a.token()).num("totalItems")).isEqualTo(2);
        assertThat(get("/api/v1/history?direction=OUT&category=TRANSFER&q=andi", a.token()).num("totalItems")).isEqualTo(1);
        assertThat(get("/api/v1/history?q=h1", a.token()).num("totalItems")).isEqualTo(1);

        Res p0 = get("/api/v1/history?size=2&page=0", a.token());
        Res p2 = get("/api/v1/history?size=2&page=2", a.token());
        assertThat(p0.at("hasMore").asBoolean()).isTrue();
        assertThat(p2.at("items").size()).isEqualTo(1);
        assertThat(p2.at("hasMore").asBoolean()).isFalse();

        // B melihat transfer dari A sebagai dana masuk
        assertThat(get("/api/v1/history?direction=IN&category=TRANSFER", b.token()).num("totalItems")).isEqualTo(1);
    }

    @Test
    void detailMemuatJurnalSeimbangDanTidakBisaDilihatOrangLain() throws Exception {
        Customer a = newCustomer();
        Customer c = newCustomer();
        Res t = transfer(a, "BCA", "1234567890", 100_000, "d1");
        Res d = get("/api/v1/history/" + t.text("id"), a.token());
        assertThat(d.text("transaction.fee")).isEqualTo("2500");
        long sum = 0;
        for (var line : d.at("journal")) sum += line.get("amount").asLong();
        assertThat(sum).isZero();
        assertThat(d.at("journal").size()).isEqualTo(3);
        assertThat(get("/api/v1/history/" + t.text("id"), c.token()).status()).isEqualTo(404);
    }

    @Test
    void ringkasanBulananDanEksporCsvPdf() throws Exception {
        Customer a = newCustomer();
        transfer(a, "BCA", "1234567890", 100_000, "m1");
        Res months = get("/api/v1/history/monthly-summary?months=3", a.token());
        assertThat(months.body().size()).isEqualTo(3);
        var current = months.body().get(2);
        assertThat(current.get("income").asLong()).isEqualTo(5_000_000);
        assertThat(current.get("expense").asLong()).isEqualTo(102_500);

        MvcResult csv = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/v1/history/export?format=csv&category=TRANSFER").header("Authorization", "Bearer " + a.token())).andReturn();
        String text = new String(csv.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertThat(csv.getResponse().getHeader("Content-Disposition")).contains("riwayat-").contains(".csv");
        assertThat(text.lines().count()).isEqualTo(2);                         // header + 1 transfer (filter aktif)
        assertThat(text).contains("\"Transfer antarbank\"").contains("-102500");

        MvcResult pdf = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/v1/history/export?format=pdf").header("Authorization", "Bearer " + a.token())).andReturn();
        assertThat(pdf.getResponse().getContentType()).isEqualTo("application/pdf");
        assertThat(new String(pdf.getResponse().getContentAsByteArray(), 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }

    // ================================================================== Notification

    @Test
    void notifikasiTransaksiMasukDanKeluarAsinkron() throws Exception {
        Customer a = newCustomer();
        Customer b = newCustomer();
        transfer(a, "DDB", b.accountNumber(), 50_000, "n1");
        Res na = waitNotifications(a, 2);   // setoran awal + transfer keluar
        Res nb = waitNotifications(b, 2);
        assertThat(na.at("items").get(0).get("title").asString()).isEqualTo("Transfer sesama Diponegoro berhasil");
        assertThat(nb.at("items").get(0).get("title").asString()).isEqualTo("Dana masuk Rp50.000");
        assertThat(nb.num("unread")).isEqualTo(2);

        // Push: diambil sekali oleh perangkat
        assertThat(get("/api/v1/me/notifications/push", b.token()).body().size()).isEqualTo(2);
        assertThat(get("/api/v1/me/notifications/push", b.token()).body().size()).isZero();
        post("/api/v1/me/notifications/read-all", b.token(), null);
        assertThat(get("/api/v1/me/notifications/unread-count", b.token()).num("unread")).isZero();
    }

    @Test
    void transferDitolakMengirimSatuNotifikasiGagal() throws Exception {
        Customer a = newCustomer();
        transfer(a, "BRI", "9999999999", 30_000, "f1");
        transfer(a, "BCA", "1234567890", 10_000, "f2");
        Res n = waitNotifications(a, 3);
        Thread.sleep(200);
        n = get("/api/v1/me/notifications", a.token());
        List<String> titles = new java.util.ArrayList<>();
        n.at("items").forEach(i -> titles.add(i.get("title").asString()));
        assertThat(titles).containsExactly("Transfer antarbank berhasil", "Transfer antarbank gagal", "Dana masuk Rp5.000.000");
    }

    @Test
    void pengaturanKanalEmailMengisiOutboxDanKanalWajibDipilih() throws Exception {
        Customer a = newCustomer();
        assertThat(put("/api/v1/me/notification-settings", a.token(), Map.of("types", List.of("OUTGOING"), "channels", List.of()))
                .status()).isEqualTo(400);
        put("/api/v1/me/notification-settings", a.token(), Map.of("types", List.of("OUTGOING"), "channels", List.of("EMAIL")));
        transfer(a, "BCA", "1234567890", 10_000, "e1");
        Res emails = null;
        for (int i = 0; i < 40; i++) {
            emails = get("/api/v1/me/emails", a.token());
            if (emails.body().size() > 0) break;
            Thread.sleep(50);
        }
        assertThat(emails.body().get(0).get("subject").asString()).isEqualTo("Transfer antarbank berhasil");
        assertThat(emails.body().get(0).get("to").asString()).isEqualTo(a.username() + "@mail.test");
    }

    // ================================================================== Profile

    @Test
    void ubahProfilDenganOtpDanUbahPasswordPin() throws Exception {
        Customer a = newCustomer();
        assertThat(post("/api/v1/me/profile-change", a.token(), Map.of("username", a.username(), "email", a.username() + "@mail.test"))
                .text("code")).isEqualTo("NO_CHANGES");
        Res req = post("/api/v1/me/profile-change", a.token(), Map.of(
                "username", a.username(), "email", a.username() + "@mail.test", "phoneNumber", "081298765432"));
        assertThat(req.text("sentTo")).isEqualTo("0812****5432");
        assertThat(post("/api/v1/me/profile-change/confirm", a.token(), Map.of("code", "000000")).status()).isEqualTo(422);
        // data belum berubah sebelum OTP valid
        assertThat(get("/api/v1/me", a.token()).text("phoneNumber")).isNull();
        Res ok = post("/api/v1/me/profile-change/confirm", a.token(), Map.of("code", req.text("demoCode")));
        assertThat(ok.text("phoneNumber")).isEqualTo("081298765432");

        assertThat(post("/api/v1/me/password", a.token(), Map.of("oldPassword", "salah", "newPassword", "BaruLagi123")).status())
                .isEqualTo(422);
        assertThat(post("/api/v1/me/password", a.token(), Map.of("oldPassword", PASSWORD, "newPassword", "BaruLagi123")).status())
                .isEqualTo(200);
        assertThat(post("/api/v1/me/pin", a.token(), Map.of("oldPin", PIN, "newPin", "654321")).status()).isEqualTo(200);
        assertThat(pay("/api/v1/transfers", a.token(), "x", Map.of("bankCode", "BCA", "accountNumber", "1234567890",
                "amount", 10_000, "pin", "654321")).text("status")).isEqualTo("SUCCESS");
    }
}
