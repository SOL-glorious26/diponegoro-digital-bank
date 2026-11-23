package com.diponegoro.ddb.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Dasar test API: Spring context penuh + H2 (profil test) + MockMvc + jam yang bisa dimajukan.
 * Menyediakan helper untuk membuat nasabah baru yang sudah lolos OTP, e-KYC, dan punya PIN.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ApiTestBase.ClockConfig.class)
public abstract class ApiTestBase {

    @TestConfiguration
    public static class ClockConfig {
        @Bean
        @Primary
        public MutableClock testClock() {
            return new MutableClock(Instant.parse("2026-11-10T03:00:00Z"));
        }
    }

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper mapper;
    @Autowired protected MutableClock clock;

    public static final String PIN = "123456";
    public static final String PASSWORD = "Rahasia123";

    /** Nasabah siap transaksi. */
    public record Customer(String userId, String token, String refreshToken, String accountNumber, String username) {}

    @BeforeEach
    void moveClockForward() {
        // Setiap test berjalan di "hari" berbeda agar data waktu tidak saling bentrok.
        clock.advance(Duration.ofHours(1));
    }

    // ------------------------------------------------------------------ HTTP helper

    protected record Res(int status, JsonNode body) {
        public String text(String path) {
            JsonNode n = at(path);
            return n == null || n.isNull() || n.isMissingNode() ? null : n.asString();
        }
        public long num(String path) { return at(path).asLong(); }
        public JsonNode at(String path) { return body.at(path.startsWith("/") ? path : "/" + path.replace('.', '/')); }
    }

    protected Res call(MockHttpServletRequestBuilder req, String token, Object body) throws Exception {
        if (token != null) req.header("Authorization", "Bearer " + token);
        if (body != null) req.contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body));
        MvcResult r = mvc.perform(req).andReturn();
        String content = r.getResponse().getContentAsString();
        JsonNode json = content.isBlank() ? mapper.createObjectNode() : mapper.readTree(content);
        return new Res(r.getResponse().getStatus(), json);
    }

    protected Res post(String url, String token, Object body) throws Exception {
        return call(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url), token, body);
    }

    /** POST transaksi keuangan dengan header Idempotency-Key. */
    protected Res pay(String url, String token, String idemKey, Object body) throws Exception {
        return call(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url)
                .header("Idempotency-Key", idemKey), token, body);
    }

    protected long balance(Customer c) throws Exception {
        return get("/api/v1/accounts/me", c.token()).num("balance");
    }

    protected Res get(String url, String token) throws Exception {
        return call(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url), token, null);
    }

    protected Res put(String url, String token, Object body) throws Exception {
        return call(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(url), token, body);
    }

    protected Res delete(String url, String token) throws Exception {
        return call(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(url), token, null);
    }

    // ------------------------------------------------------------------ alur nasabah

    protected static String uniq() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    /** Registrasi + OTP → token sesi (status ACTIVE, belum e-KYC). */
    protected Customer registerAndVerify(String username) throws Exception {
        Res reg = post("/api/v1/auth/register", null, Map.of(
                "username", username, "email", username + "@mail.test", "password", PASSWORD));
        if (reg.status() != 201) throw new AssertionError("register gagal: " + reg.body());
        String userId = reg.text("userId");
        Res ver = post("/api/v1/auth/register/verify-otp", null, Map.of(
                "userId", userId, "code", reg.text("otp.demoCode"), "deviceId", "dev-" + username));
        if (ver.status() != 200) throw new AssertionError("verify gagal: " + ver.body());
        return new Customer(userId, ver.text("accessToken"), ver.text("refreshToken"), null, username);
    }

    /** Nasabah lengkap: OTP, e-KYC disetujui (rekening dibuka dengan Rp5 juta khusus profil test), PIN 123456. */
    protected Customer newCustomer() throws Exception {
        String username = "u" + uniq();
        Customer c = registerAndVerify(username);
        Res ekyc = post("/api/v1/auth/ekyc", c.token(), Map.of(
                "nik", "3374010101" + (100000 + (int) (Math.random() * 800000)), "fullName", "Nasabah " + username,
                "selfieReference", "selfie.jpg"));
        if (ekyc.status() != 200) throw new AssertionError("ekyc gagal: " + ekyc.body());
        clock.advance(Duration.ofSeconds(4));
        Res status = get("/api/v1/auth/ekyc", c.token());
        if (!"APPROVED".equals(status.text("status"))) throw new AssertionError("ekyc belum approved: " + status.body());
        Res pin = post("/api/v1/auth/pin", c.token(), Map.of("pin", PIN));
        if (pin.status() != 201) throw new AssertionError("pin gagal: " + pin.body());
        Res acc = get("/api/v1/accounts/me", c.token());
        return new Customer(c.userId(), c.token(), c.refreshToken(), acc.text("accountNumber"), username);
    }
}
