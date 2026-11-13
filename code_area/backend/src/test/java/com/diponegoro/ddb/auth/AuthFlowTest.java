package com.diponegoro.ddb.auth;

import com.diponegoro.ddb.support.ApiTestBase;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuthFlowTest extends ApiTestBase {

    @Test
    void verifikasiPinMandiriMemakaiAturanBlokirYangSama() throws Exception {
        Customer c = newCustomer();
        assertThat(post("/api/v1/auth/pin/verify", c.token(), Map.of("pin", PIN)).status()).isEqualTo(200);
        Res wrong1 = post("/api/v1/auth/pin/verify", c.token(), Map.of("pin", "000000"));
        assertThat(wrong1.status()).isEqualTo(422);
        assertThat(wrong1.text("code")).isEqualTo("PIN_INVALID");
        assertThat(wrong1.text("message")).contains("Sisa percobaan: 2");
        post("/api/v1/auth/pin/verify", c.token(), Map.of("pin", "000000"));
        Res blocked = post("/api/v1/auth/pin/verify", c.token(), Map.of("pin", "000000"));
        assertThat(blocked.status()).isEqualTo(423);
        assertThat(blocked.text("code")).isEqualTo("PIN_BLOCKED");
        // PIN benar pun ditolak selama blokir, lalu diterima setelah 5 menit
        assertThat(post("/api/v1/auth/pin/verify", c.token(), Map.of("pin", PIN)).status()).isEqualTo(423);
        clock.advance(Duration.ofMinutes(5).plusSeconds(1));
        Res relogin = post("/api/v1/auth/refresh", null, Map.of("refreshToken", c.refreshToken()));
        assertThat(post("/api/v1/auth/pin/verify", relogin.text("accessToken"), Map.of("pin", PIN)).status()).isEqualTo(200);
    }

    @Test
    void registrasiOtpEkycPinSampaiFinancialEnabled() throws Exception {
        String username = "a" + uniq();
        Res reg = post("/api/v1/auth/register", null, Map.of(
                "username", username, "email", username + "@mail.test", "password", PASSWORD));
        assertThat(reg.status()).isEqualTo(201);
        assertThat(reg.text("otp.demoCode")).matches("\\d{6}");
        assertThat(reg.text("otp.sentTo")).contains("**@mail.test");

        // Login sebelum OTP ditolak
        Res early = post("/api/v1/auth/login", null, Map.of("identifier", username, "password", PASSWORD, "deviceId", "d1"));
        assertThat(early.status()).isEqualTo(403);
        assertThat(early.text("code")).isEqualTo("OTP_REQUIRED");

        // OTP salah
        String wrong = reg.text("otp.demoCode").equals("000000") ? "111111" : "000000";
        Res bad = post("/api/v1/auth/register/verify-otp", null, Map.of(
                "userId", reg.text("userId"), "code", wrong, "deviceId", "d1"));
        assertThat(bad.status()).isEqualTo(422);
        assertThat(bad.text("message")).isEqualTo("Kode OTP salah.");

        // Kirim ulang: OTP lama tidak berlaku
        Res resend = post("/api/v1/auth/register/resend-otp", null, Map.of("userId", reg.text("userId")));
        Res old = post("/api/v1/auth/register/verify-otp", null, Map.of(
                "userId", reg.text("userId"), "code", reg.text("otp.demoCode"), "deviceId", "d1"));
        if (!reg.text("otp.demoCode").equals(resend.text("demoCode"))) assertThat(old.status()).isEqualTo(422);

        Res ok = post("/api/v1/auth/register/verify-otp", null, Map.of(
                "userId", reg.text("userId"), "code", resend.text("demoCode"), "deviceId", "d1"));
        assertThat(ok.status()).isEqualTo(200);
        assertThat(ok.text("user.status")).isEqualTo("ACTIVE");
        String token = ok.text("accessToken");

        // e-KYC: diproses dulu, lalu disetujui
        Res ekyc = post("/api/v1/auth/ekyc", token, Map.of("nik", "3374010101010001", "selfieReference", "s.jpg"));
        assertThat(ekyc.text("status")).isEqualTo("PROCESSING");
        assertThat(ekyc.text("nikMasked")).isEqualTo("************0001");
        assertThat(get("/api/v1/auth/ekyc", token).text("status")).isEqualTo("PROCESSING");
        clock.advance(Duration.ofSeconds(4));
        assertThat(get("/api/v1/auth/ekyc", token).text("status")).isEqualTo("APPROVED");

        assertThat(post("/api/v1/auth/pin", token, Map.of("pin", "12345")).status()).isEqualTo(400);
        assertThat(post("/api/v1/auth/pin", token, Map.of("pin", PIN)).status()).isEqualTo(201);
        assertThat(post("/api/v1/auth/pin", token, Map.of("pin", PIN)).status()).isEqualTo(409);

        Res login = post("/api/v1/auth/login", null, Map.of("identifier", username + "@MAIL.test", "password", PASSWORD, "deviceId", "d2"));
        assertThat(login.status()).isEqualTo(200);
        assertThat(login.text("user.status")).isEqualTo("FINANCIAL_ENABLED");
        assertThat(login.at("user.hasPin").asBoolean()).isTrue();
    }

    @Test
    void ekycDenganNikBerakhiran0000Ditolak() throws Exception {
        Customer c = registerAndVerify("r" + uniq());
        post("/api/v1/auth/ekyc", c.token(), Map.of("nik", "3374010101010000", "selfieReference", "s.jpg"));
        clock.advance(Duration.ofSeconds(4));
        Res st = get("/api/v1/auth/ekyc", c.token());
        assertThat(st.text("status")).isEqualTo("REJECTED");
        assertThat(st.text("reason")).isNotBlank();
    }

    @Test
    void passwordSalahPesanUmumDanDikunciSetelah5Kali() throws Exception {
        Customer c = registerAndVerify("p" + uniq());
        Res unknown = post("/api/v1/auth/login", null, Map.of("identifier", "tidakada" + uniq(), "password", "x", "deviceId", "d"));
        Res wrong = post("/api/v1/auth/login", null, Map.of("identifier", c.username(), "password", "Salah12345", "deviceId", "d"));
        assertThat(unknown.status()).isEqualTo(401);
        assertThat(wrong.status()).isEqualTo(401);
        assertThat(unknown.text("message")).isEqualTo(wrong.text("message")); // tidak membocorkan keberadaan akun
        for (int i = 0; i < 4; i++) {
            post("/api/v1/auth/login", null, Map.of("identifier", c.username(), "password", "Salah12345", "deviceId", "d"));
        }
        Res locked = post("/api/v1/auth/login", null, Map.of("identifier", c.username(), "password", PASSWORD, "deviceId", "d"));
        assertThat(locked.status()).isEqualTo(423);
        clock.advance(Duration.ofMinutes(16));
        assertThat(post("/api/v1/auth/login", null, Map.of("identifier", c.username(), "password", PASSWORD, "deviceId", "d")).status())
                .isEqualTo(200);
    }

    @Test
    void logoutSemuaPerangkatMembatalkanSemuaToken() throws Exception {
        Customer c = registerAndVerify("l" + uniq());
        Res second = post("/api/v1/auth/login", null, Map.of("identifier", c.username(), "password", PASSWORD, "deviceId", "hp-2"));
        assertThat(get("/api/v1/auth/sessions", c.token()).body().size()).isEqualTo(2);

        assertThat(post("/api/v1/auth/logout-all", second.text("accessToken"), null).status()).isEqualTo(200);
        assertThat(get("/api/v1/auth/sessions", c.token()).status()).isEqualTo(401);
        assertThat(get("/api/v1/auth/sessions", second.text("accessToken")).status()).isEqualTo(401);
        assertThat(post("/api/v1/auth/refresh", null, Map.of("refreshToken", c.refreshToken())).status()).isEqualTo(401);
    }

    @Test
    void sesiBerakhirSetelah5MenitTidakAktifTetapiRefreshTokenDirotasi() throws Exception {
        Customer c = registerAndVerify("s" + uniq());
        Res r1 = post("/api/v1/auth/refresh", null, Map.of("refreshToken", c.refreshToken()));
        assertThat(r1.status()).isEqualTo(200);
        // refresh token lama tidak bisa dipakai ulang
        assertThat(post("/api/v1/auth/refresh", null, Map.of("refreshToken", c.refreshToken())).status()).isEqualTo(401);

        clock.advance(Duration.ofMinutes(6));
        assertThat(get("/api/v1/auth/sessions", r1.text("accessToken")).status()).isEqualTo(401);
    }

    @Test
    void pemulihanPasswordDenganOtp() throws Exception {
        Customer c = registerAndVerify("v" + uniq());
        Res start = post("/api/v1/auth/recovery/request", null, Map.of("identifier", c.username(), "type", "PASSWORD"));
        Res unknown = post("/api/v1/auth/recovery/request", null, Map.of("identifier", "x" + uniq(), "type", "PASSWORD"));
        assertThat(start.text("message")).isEqualTo(unknown.text("message"));

        Res ver = post("/api/v1/auth/recovery/verify", null, Map.of(
                "identifier", c.username(), "type", "PASSWORD", "code", start.text("otp.demoCode")));
        assertThat(ver.status()).isEqualTo(200);
        Res reset = post("/api/v1/auth/recovery/reset", null, Map.of("identifier", c.username(), "type", "PASSWORD",
                "recoveryToken", ver.text("recoveryToken"), "newSecret", "BaruSekali99"));
        assertThat(reset.status()).isEqualTo(200);
        // token sekali pakai
        assertThat(post("/api/v1/auth/recovery/reset", null, Map.of("identifier", c.username(), "type", "PASSWORD",
                "recoveryToken", ver.text("recoveryToken"), "newSecret", "LainLagi99")).status()).isEqualTo(422);
        // semua sesi lama berakhir, password baru berlaku
        assertThat(get("/api/v1/auth/sessions", c.token()).status()).isEqualTo(401);
        assertThat(post("/api/v1/auth/login", null, Map.of("identifier", c.username(), "password", "BaruSekali99", "deviceId", "d")).status())
                .isEqualTo(200);
    }

    @Test
    void loginBiometrikDenganTokenPerangkat() throws Exception {
        Customer c = registerAndVerify("b" + uniq());
        Res enroll = post("/api/v1/auth/biometric/enroll", c.token(), Map.of("deviceId", "hp-bio"));
        assertThat(enroll.status()).isEqualTo(200);
        String bio = enroll.text("biometricToken");
        post("/api/v1/auth/logout", c.token(), null);

        assertThat(post("/api/v1/auth/biometric-login", null, Map.of("biometricToken", bio, "deviceId", "hp-lain")).status())
                .isEqualTo(401);
        Res ok = post("/api/v1/auth/biometric-login", null, Map.of("biometricToken", bio, "deviceId", "hp-bio"));
        assertThat(ok.status()).isEqualTo(200);

        post("/api/v1/auth/biometric/disable", ok.text("accessToken"), null);
        assertThat(post("/api/v1/auth/biometric-login", null, Map.of("biometricToken", bio, "deviceId", "hp-bio")).status())
                .isEqualTo(401);
    }

    @Test
    void endpointTerlindungTanpaTokenDitolakDenganJsonSeragam() throws Exception {
        Res r = get("/api/v1/auth/sessions", null);
        assertThat(r.status()).isEqualTo(401);
        assertThat(r.text("code")).isEqualTo("UNAUTHORIZED");
        assertThat(get("/api/v1/auth/sessions", "bukan.token.valid").status()).isEqualTo(401);
    }
}
