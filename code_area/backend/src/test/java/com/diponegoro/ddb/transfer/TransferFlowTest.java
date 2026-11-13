package com.diponegoro.ddb.transfer;

import com.diponegoro.ddb.support.ApiTestBase;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TransferFlowTest extends ApiTestBase {

    private Map<String, Object> req(String bank, String number, long amount, String pin) {
        return Map.of("bankCode", bank, "accountNumber", number, "amount", amount, "note", "Uji", "pin", pin);
    }

    @Test
    void transferSesamaBankAtomikDanKeduaSaldoBerubah() throws Exception {
        Customer a = newCustomer();
        Customer b = newCustomer();
        Res inq = post("/api/v1/transfers/inquiry", a.token(), Map.of("bankCode", "DDB", "accountNumber", b.accountNumber()));
        assertThat(inq.text("name")).isEqualTo(("Nasabah " + b.username()).toUpperCase());

        Res q = post("/api/v1/transfers/quote", a.token(), Map.of("bankCode", "DDB", "accountNumber", b.accountNumber(), "amount", 150_000));
        assertThat(q.num("fee")).isZero();
        assertThat(q.num("balanceAfter")).isEqualTo(4_850_000);

        Res t = pay("/api/v1/transfers", a.token(), "trf-1", req("DDB", b.accountNumber(), 150_000, PIN));
        assertThat(t.status()).isEqualTo(200);
        assertThat(t.text("status")).isEqualTo("SUCCESS");
        assertThat(t.text("direction")).isEqualTo("OUT");
        assertThat(t.text("reference")).matches("DDB\\d{6}[0-9A-F]{8}");
        assertThat(balance(a)).isEqualTo(4_850_000);
        assertThat(balance(b)).isEqualTo(5_150_000);

        // retry dengan kunci sama → transaksi yang sama, tanpa debit ganda (BR-04)
        Res again = pay("/api/v1/transfers", a.token(), "trf-1", req("DDB", b.accountNumber(), 150_000, PIN));
        assertThat(again.text("id")).isEqualTo(t.text("id"));
        assertThat(balance(a)).isEqualTo(4_850_000);
    }

    @Test
    void validasiInputA2A3DanRekeningSendiri() throws Exception {
        Customer a = newCustomer();
        assertThat(post("/api/v1/transfers/quote", a.token(), Map.of("bankCode", "DDB", "accountNumber", "1400000999", "amount", 10_000))
                .text("code")).isEqualTo("INVALID_ACCOUNT");
        assertThat(post("/api/v1/transfers/inquiry", a.token(), Map.of("bankCode", "DDB", "accountNumber", a.accountNumber()))
                .text("code")).isEqualTo("OWN_ACCOUNT");
        Res small = post("/api/v1/transfers/quote", a.token(), Map.of("bankCode", "BCA", "accountNumber", "1234567890", "amount", 4_999));
        assertThat(small.text("message")).isEqualTo("Nominal minimal Rp5.000");
        Res big = post("/api/v1/transfers/quote", a.token(), Map.of("bankCode", "BCA", "accountNumber", "1234567890", "amount", 25_000_001));
        assertThat(big.text("code")).isEqualTo("AMOUNT_TOO_LARGE");
    }

    @Test
    void saldoTidakCukupA1() throws Exception {
        Customer a = newCustomer();
        Res r = pay("/api/v1/transfers", a.token(), "x", req("BCA", "1234567890", 4_999_000, PIN));
        assertThat(r.status()).isEqualTo(422);
        assertThat(r.text("message")).startsWith("Saldo tidak mencukupi. Saldo Anda Rp5.000.000, dibutuhkan Rp5.001.500");
        assertThat(balance(a)).isEqualTo(5_000_000);
    }

    @Test
    void pinSalahA4TidakMendebitDanBlokirSetelah3Kali() throws Exception {
        Customer a = newCustomer();
        assertThat(pay("/api/v1/transfers", a.token(), "p1", req("BCA", "1234567890", 10_000, "111111")).text("message"))
                .isEqualTo("PIN salah. Sisa percobaan: 2");
        pay("/api/v1/transfers", a.token(), "p2", req("BCA", "1234567890", 10_000, "111111"));
        Res blocked = pay("/api/v1/transfers", a.token(), "p3", req("BCA", "1234567890", 10_000, "111111"));
        assertThat(blocked.status()).isEqualTo(423);
        // PIN benar pun ditolak selama blokir
        assertThat(pay("/api/v1/transfers", a.token(), "p4", req("BCA", "1234567890", 10_000, PIN)).text("code"))
                .isEqualTo("PIN_BLOCKED");
        assertThat(balance(a)).isEqualTo(5_000_000);

        clock.advance(Duration.ofMinutes(4));
        get("/api/v1/accounts/me", a.token()); // jaga sesi tetap aktif
        clock.advance(Duration.ofMinutes(2));
        assertThat(pay("/api/v1/transfers", a.token(), "p5", req("BCA", "1234567890", 10_000, PIN)).text("status"))
                .isEqualTo("SUCCESS");
    }

    @Test
    void antarbankDenganBiaya() throws Exception {
        Customer a = newCustomer();
        Res t = pay("/api/v1/transfers", a.token(), "ib", req("BCA", "1234567890", 100_000, PIN));
        assertThat(t.text("status")).isEqualTo("SUCCESS");
        assertThat(t.num("fee")).isEqualTo(2_500);
        assertThat(t.text("toDetail")).isEqualTo("BCA • 1234567890");
        assertThat(balance(a)).isEqualTo(5_000_000 - 102_500);
    }

    @Test
    void ditolakJaringanA6DanaDikembalikan() throws Exception {
        Customer a = newCustomer();
        Res t = pay("/api/v1/transfers", a.token(), "rej", req("BRI", "9999999999", 50_000, PIN));
        assertThat(t.text("status")).isEqualTo("FAILED");
        assertThat(t.text("statusReason")).isEqualTo("Ditolak oleh jaringan antarbank");
        assertThat(balance(a)).isEqualTo(5_000_000);
    }

    @Test
    void timeoutA5DiprosesLaluCekUlangBerhasil() throws Exception {
        Customer a = newCustomer();
        Res t = pay("/api/v1/transfers", a.token(), "to", req("MANDIRI", "8888888888", 50_000, PIN));
        assertThat(t.text("status")).isEqualTo("PROCESSING");
        assertThat(balance(a)).isEqualTo(5_000_000 - 52_500);
        // retry dengan kunci yang sama tidak mendebit lagi
        pay("/api/v1/transfers", a.token(), "to", req("MANDIRI", "8888888888", 50_000, PIN));
        assertThat(balance(a)).isEqualTo(5_000_000 - 52_500);
        Res st = post("/api/v1/transfers/" + t.text("id") + "/check-status", a.token(), null);
        assertThat(st.text("status")).isEqualTo("SUCCESS");
    }

    @Test
    void favoritTambahHapusDanTerlihatSaatInquiry() throws Exception {
        Customer a = newCustomer();
        Res f = post("/api/v1/transfers/favorites", a.token(), Map.of("bankCode", "BNI", "accountNumber", "1122334455"));
        assertThat(f.status()).isEqualTo(201);
        assertThat(f.text("name")).isEqualTo("CITRA AYU");
        assertThat(post("/api/v1/transfers/favorites", a.token(), Map.of("bankCode", "BNI", "accountNumber", "1122334455")).status())
                .isEqualTo(409);
        assertThat(post("/api/v1/transfers/inquiry", a.token(), Map.of("bankCode", "BNI", "accountNumber", "1122334455"))
                .at("favorite").asBoolean()).isTrue();
        delete("/api/v1/transfers/favorites/" + f.text("id"), a.token());
        assertThat(get("/api/v1/transfers/favorites", a.token()).body().size()).isZero();
    }

    @Test
    void transferParalelTidakMenembusSaldoDanKunciSamaHanyaSekaliDebit() throws Exception {
        Customer a = newCustomer();
        Customer b = newCustomer();
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(8);
        try {
            // 8 transfer Rp1.000.000 paralel dari saldo Rp5.000.000 → tepat 5 berhasil.
            java.util.List<java.util.concurrent.Future<Res>> futures = new java.util.ArrayList<>();
            for (int i = 0; i < 8; i++) {
                String key = "par-" + i;
                futures.add(pool.submit(() -> pay("/api/v1/transfers", a.token(), key, req("DDB", b.accountNumber(), 1_000_000, PIN))));
            }
            int ok = 0;
            for (var f : futures) if (f.get().status() == 200) ok++;
            assertThat(ok).isEqualTo(5);
            assertThat(balance(a)).isZero();
            assertThat(balance(b)).isEqualTo(10_000_000);

            // 6 permintaan paralel dengan kunci idempotensi yang sama → satu transaksi
            java.util.List<java.util.concurrent.Future<Res>> same = new java.util.ArrayList<>();
            for (int i = 0; i < 6; i++) {
                same.add(pool.submit(() -> pay("/api/v1/transfers", b.token(), "same-key", req("BCA", "1234567890", 100_000, PIN))));
            }
            java.util.Set<String> ids = new java.util.HashSet<>();
            for (var f : same) {
                Res r = f.get();
                if (r.status() == 200) ids.add(r.text("id"));
            }
            assertThat(ids).hasSize(1);
            assertThat(balance(b)).isEqualTo(10_000_000 - 102_500);
        } finally {
            pool.shutdown();
        }
    }

    @Test
    void belumEkycTidakBisaTransfer() throws Exception {
        Customer c = registerAndVerify("n" + uniq());
        Res r = pay("/api/v1/transfers", c.token(), "k", req("BCA", "1234567890", 10_000, PIN));
        assertThat(r.status()).isEqualTo(403);
    }
}
