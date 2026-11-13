package com.diponegoro.ddb.security;

import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;

import com.diponegoro.ddb.support.ApiTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/** DEF-04: CORS untuk aplikasi Web; origin selain yang diizinkan ditolak. */
class CorsTest extends ApiTestBase {

    @Test
    void preflightDariOriginWebDiizinkanDanOriginLainDitolak() throws Exception {
        MvcResult ok = mvc.perform(options("/api/v1/transfers")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Authorization, Content-Type, Idempotency-Key"))
                .andReturn();
        assertThat(ok.getResponse().getStatus()).isEqualTo(200);
        assertThat(ok.getResponse().getHeader("Access-Control-Allow-Origin")).isEqualTo("http://localhost:5173");
        assertThat(ok.getResponse().getHeader("Access-Control-Allow-Headers")).containsIgnoringCase("Idempotency-Key");

        MvcResult bad = mvc.perform(options("/api/v1/transfers")
                        .header("Origin", "https://situs-lain.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andReturn();
        assertThat(bad.getResponse().getStatus()).isEqualTo(403);
        assertThat(bad.getResponse().getHeader("Access-Control-Allow-Origin")).isNull();
    }

    @Test
    void responsBiasaMemuatHeaderCorsDanTanpaOriginTidakTerpengaruh() throws Exception {
        MvcResult web = mvc.perform(MockMvcRequestBuilders.get("/api/v1/transfers/banks").header("Origin", "http://127.0.0.1:4173")).andReturn();
        assertThat(web.getResponse().getStatus()).isEqualTo(401);   // belum login, tetapi header CORS tetap ada
        assertThat(web.getResponse().getHeader("Access-Control-Allow-Origin")).isEqualTo("http://127.0.0.1:4173");
        assertThat(web.getResponse().getHeader("Access-Control-Expose-Headers")).contains("Content-Disposition");

        // Klien non-peramban (aplikasi Android) tidak mengirim Origin
        MvcResult android = mvc.perform(MockMvcRequestBuilders.get("/api/v1/transfers/banks")).andReturn();
        assertThat(android.getResponse().getStatus()).isEqualTo(401);
        assertThat(android.getResponse().getHeader("Access-Control-Allow-Origin")).isNull();
    }
}
