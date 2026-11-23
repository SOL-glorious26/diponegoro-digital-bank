package com.diponegoro.ddb.account;

import com.diponegoro.ddb.support.ApiTestBase;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AccountFlowTest extends ApiTestBase {

    @Test
    void rekeningDibukaSetelahEkycDenganSaldoAwal() throws Exception {
        Customer c = newCustomer();
        Res acc = get("/api/v1/accounts/me", c.token());
        assertThat(acc.status()).isEqualTo(200);
        // Profil test memakai Rp5 juta (ddb.opening-balance) agar skenario transfer cukup saldo.
        assertThat(acc.num("balance")).isEqualTo(5_000_000L);
        // Bawaan aplikasi: Rp50.000, ketentuan PT Diponegoro Digital Bank.
        assertThat(com.diponegoro.ddb.auth.AuthService.NEW_USER_OPENING_BALANCE).isEqualTo(50_000L);
        assertThat(acc.text("accountNumber")).matches("1400\\d{6}");
        assertThat(acc.text("ownerName")).isEqualTo(("Nasabah " + c.username()).toUpperCase());
    }

    @Test
    void rekeningBelumAdaSebelumEkyc() throws Exception {
        Customer c = registerAndVerify("k" + uniq());
        Res acc = get("/api/v1/accounts/me", c.token());
        assertThat(acc.status()).isEqualTo(403);
        assertThat(acc.text("code")).isEqualTo("ACCOUNT_NOT_OPEN");
    }

    @Test
    void sembunyikanSaldoTersimpan() throws Exception {
        Customer c = newCustomer();
        put("/api/v1/accounts/me/balance-visibility", c.token(), Map.of("hidden", true));
        assertThat(get("/api/v1/accounts/me", c.token()).at("balanceHidden").asBoolean()).isTrue();
    }

    @Test
    void kantongIsiKunciTarikDiniDenganPin() throws Exception {
        Customer c = newCustomer();
        Res p = post("/api/v1/accounts/pockets", c.token(), Map.of(
                "name", "Liburan", "purpose", "Bali 2027", "target", 5_000_000,
                "lockType", "DATE", "lockUntil", LocalDate.of(2026, 12, 10).toString()));
        assertThat(p.status()).isEqualTo(201);
        assertThat(p.at("locked").asBoolean()).isTrue();
        String id = p.text("id");

        // Isi Rp1.000.000 — idempoten: kunci sama dua kali = satu debit
        Res d1 = pay("/api/v1/accounts/pockets/" + id + "/deposit", c.token(), "dep-1", Map.of("amount", 1_000_000));
        Res d2 = pay("/api/v1/accounts/pockets/" + id + "/deposit", c.token(), "dep-1", Map.of("amount", 1_000_000));
        assertThat(d1.status()).isEqualTo(200);
        assertThat(d2.num("balance")).isEqualTo(1_000_000);
        assertThat(balance(c)).isEqualTo(4_000_000);
        assertThat(d1.at("progress").asDouble()).isEqualTo(0.2);

        // Tanpa header idempotensi ditolak
        assertThat(post("/api/v1/accounts/pockets/" + id + "/deposit", c.token(), Map.of("amount", 1000)).status())
                .isEqualTo(400);

        // Terkunci: tarik biasa ditolak, tarik dini butuh PIN
        Res plain = pay("/api/v1/accounts/pockets/" + id + "/withdraw", c.token(), "w-1", Map.of("amount", 200_000));
        assertThat(plain.text("code")).isEqualTo("POCKET_LOCKED");
        Res wrongPin = pay("/api/v1/accounts/pockets/" + id + "/withdraw", c.token(), "w-2",
                Map.of("amount", 200_000, "earlyWithdrawal", true, "pin", "999999"));
        assertThat(wrongPin.text("message")).isEqualTo("PIN salah. Sisa percobaan: 2");
        Res early = pay("/api/v1/accounts/pockets/" + id + "/withdraw", c.token(), "w-3",
                Map.of("amount", 200_000, "earlyWithdrawal", true, "pin", PIN));
        assertThat(early.status()).isEqualTo(200);
        assertThat(early.num("balance")).isEqualTo(800_000);
        assertThat(balance(c)).isEqualTo(4_200_000);

        // Kantong terkunci tidak bisa dihapus
        Res del = call(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .delete("/api/v1/accounts/pockets/" + id).header("Idempotency-Key", "del-1"), c.token(), null);
        assertThat(del.status()).isEqualTo(422);
    }

    @Test
    void kunciTargetTerbukaOtomatisSaatTargetTercapaiLaluHapusMengembalikanSaldo() throws Exception {
        Customer c = newCustomer();
        Res p = post("/api/v1/accounts/pockets", c.token(), Map.of("name", "HP baru", "target", 100_000, "lockType", "TARGET"));
        String id = p.text("id");
        pay("/api/v1/accounts/pockets/" + id + "/deposit", c.token(), "a", Map.of("amount", 60_000));
        assertThat(get("/api/v1/accounts/pockets/" + id, c.token()).at("locked").asBoolean()).isTrue();
        pay("/api/v1/accounts/pockets/" + id + "/deposit", c.token(), "b", Map.of("amount", 40_000));
        assertThat(get("/api/v1/accounts/pockets/" + id, c.token()).at("locked").asBoolean()).isFalse();

        Res del = call(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .delete("/api/v1/accounts/pockets/" + id).header("Idempotency-Key", "del"), c.token(), null);
        assertThat(del.status()).isEqualTo(204);
        assertThat(balance(c)).isEqualTo(5_000_000);
        assertThat(get("/api/v1/accounts/pockets", c.token()).body().size()).isZero();
    }

    @Test
    void kunciTanggalTerbukaSetelahTanggalLewat() throws Exception {
        Customer c = newCustomer();
        LocalDate besok = LocalDate.ofInstant(clock.instant(), java.time.ZoneId.of("Asia/Jakarta")).plusDays(1);
        Res p = post("/api/v1/accounts/pockets", c.token(), Map.of("name", "Dana darurat", "lockType", "DATE", "lockUntil", besok.toString()));
        assertThat(p.at("locked").asBoolean()).isTrue();
        clock.advance(Duration.ofDays(1));
        Customer again = c; // token kedaluwarsa karena idle; login ulang
        Res login = post("/api/v1/auth/login", null, Map.of("identifier", c.username(), "password", PASSWORD, "deviceId", "d"));
        assertThat(get("/api/v1/accounts/pockets/" + p.text("id"), login.text("accessToken")).at("locked").asBoolean()).isFalse();
    }

    @Test
    void saldoTidakCukupTidakMengubahApaPun() throws Exception {
        Customer c = newCustomer();
        Res p = post("/api/v1/accounts/pockets", c.token(), Map.of("name", "Besar"));
        Res r = pay("/api/v1/accounts/pockets/" + p.text("id") + "/deposit", c.token(), "x", Map.of("amount", 6_000_000));
        assertThat(r.status()).isEqualTo(422);
        assertThat(r.text("message")).isEqualTo("Saldo tidak mencukupi");
        assertThat(balance(c)).isEqualTo(5_000_000);
    }

    @Test
    void grafikSaldoKonsistenDenganLedger() throws Exception {
        Customer c = newCustomer();
        Res chart = get("/api/v1/accounts/me/balance-chart?period=DAILY", c.token());
        assertThat(chart.at("points").size()).isEqualTo(14);
        assertThat(chart.at("points").get(13).get("balance").asLong()).isEqualTo(5_000_000);
        assertThat(get("/api/v1/accounts/me/balance-chart?period=MONTHLY", c.token()).at("points").size()).isEqualTo(6);
    }

    @Test
    void kantongOrangLainTidakBisaDiakses() throws Exception {
        Customer a = newCustomer();
        Customer b = newCustomer();
        String id = post("/api/v1/accounts/pockets", a.token(), Map.of("name", "Rahasia")).text("id");
        assertThat(get("/api/v1/accounts/pockets/" + id, b.token()).status()).isEqualTo(404);
        assertThat(pay("/api/v1/accounts/pockets/" + id + "/withdraw", b.token(), "z", Map.of("amount", 1)).status())
                .isEqualTo(404);
    }
}
