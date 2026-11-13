package com.diponegoro.ddb.security;

import com.diponegoro.ddb.common.ErrorResponse;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class SecurityConfig {

    /** Endpoint yang boleh diakses tanpa login. */
    static final String[] PUBLIC = {
            "/api/v1/auth/register", "/api/v1/auth/register/verify-otp", "/api/v1/auth/register/resend-otp",
            "/api/v1/auth/login", "/api/v1/auth/biometric-login", "/api/v1/auth/refresh",
            "/api/v1/auth/recovery/**",
            "/actuator/health/**", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**", "/error"
    };

    @Bean
    SecurityFilterChain api(HttpSecurity http, JwtService jwt, SessionJwtConverter converter, ObjectMapper mapper)
            throws Exception {
        AuthenticationEntryPoint unauthorized = (req, res, ex) -> write(res, mapper, HttpStatus.UNAUTHORIZED,
                ErrorResponse.of("UNAUTHORIZED", "Sesi tidak valid atau sudah berakhir. Silakan login kembali."));
        AccessDeniedHandler forbidden = (req, res, ex) -> write(res, mapper, HttpStatus.FORBIDDEN,
                ErrorResponse.of("FORBIDDEN", "Akses ditolak"));
        http
                .csrf(c -> c.disable())                // API stateless dengan Bearer token
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(PUBLIC).permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(o -> o
                        .jwt(j -> j.decoder(jwt.decoder()).jwtAuthenticationConverter(converter))
                        .authenticationEntryPoint(unauthorized)
                        .accessDeniedHandler(forbidden))
                .exceptionHandling(e -> e.authenticationEntryPoint(unauthorized).accessDeniedHandler(forbidden));
        return http.build();
    }

    /**
     * CORS untuk aplikasi Web (DEF-04). Origin yang diizinkan dibaca dari {@code ddb.cors.allowed-origins}
     * (env {@code DDB_CORS_ALLOWED_ORIGINS}, dipisah koma; pola seperti {@code http://localhost:*} didukung).
     * Aplikasi Android tidak mengirim header Origin sehingga tidak terpengaruh.
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${ddb.cors.allowed-origins:http://localhost:*,http://127.0.0.1:*}") String allowedOrigins) {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOriginPatterns(Arrays.stream(allowedOrigins.split(",")).map(String::trim).filter(o -> !o.isEmpty()).toList());
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key"));
        c.setExposedHeaders(List.of("Content-Disposition"));   // nama berkas ekspor riwayat (FR-26)
        c.setAllowCredentials(false);                           // token dikirim lewat header, bukan cookie
        c.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", c);
        return source;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private static void write(jakarta.servlet.http.HttpServletResponse res, ObjectMapper mapper, HttpStatus status,
                              ErrorResponse body) throws java.io.IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        mapper.writeValue(res.getOutputStream(), body);
    }
}
