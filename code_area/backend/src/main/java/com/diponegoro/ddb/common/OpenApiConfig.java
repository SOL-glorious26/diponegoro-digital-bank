package com.diponegoro.ddb.common;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Kontrak API bersama Mobile & Web (SRS 2.5, NFR-12): /v3/api-docs dan /swagger-ui.html. */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI ddbOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Diponegoro Digital Bank API")
                        .version("v1")
                        .description("""
                                Backend bersama aplikasi Mobile & Web (SRS-DDB-001 v0.1).

                                **Cara mencoba:** POST /api/v1/auth/login dengan `{"identifier":"demo","password":"Demo@1234","deviceId":"swagger"}`, \
                                salin `accessToken`, klik **Authorize**.

                                **Aturan umum:** semua uang dalam rupiah (bilangan bulat); waktu dalam ISO-8601 UTC; \
                                setiap transaksi keuangan wajib header `Idempotency-Key` (UUID per transaksi, dipakai ulang saat retry); \
                                error berbentuk `{code, message, fields?, timestamp}`.
                                """))
                .components(new Components().addSecuritySchemes("bearer",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer"));
    }
}
