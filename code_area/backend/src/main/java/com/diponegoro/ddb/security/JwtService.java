package com.diponegoro.ddb.security;

import com.diponegoro.ddb.common.DdbProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Access token JWT berumur pendek (NFR-06). Klaim: sub = userId, sid = sessionId.
 * Token yang sesinya sudah diakhiri (logout / logout semua perangkat) ditolak oleh
 * {@link SessionJwtConverter} walaupun belum kedaluwarsa (FR-05).
 */
@Service
public class JwtService {
    public static final String ISSUER = "ddb-backend";
    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final Duration accessTtl;
    private final Clock clock;

    public JwtService(DdbProperties props, Clock clock) {
        String secret = props.security().jwtSecret();
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("ddb.security.jwt-secret minimal 32 karakter");
        }
        SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        NimbusJwtDecoder nimbus = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        JwtTimestampValidator timestamps = new JwtTimestampValidator(Duration.ofSeconds(30));
        timestamps.setClock(clock);
        nimbus.setJwtValidator(new DelegatingOAuth2TokenValidator<>(timestamps, new JwtIssuerValidator(ISSUER)));
        this.decoder = nimbus;
        this.accessTtl = Duration.ofMinutes(props.security().accessTokenMinutes());
        this.clock = clock;
    }

    public record AccessToken(String token, Instant expiresAt) {}

    public AccessToken issue(String userId, String sessionId) {
        Instant now = clock.instant();
        Instant exp = now.plus(accessTtl);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(userId)
                .issuedAt(now)
                .expiresAt(exp)
                .claim("sid", sessionId)
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return new AccessToken(token, exp);
    }

    public JwtDecoder decoder() {
        return decoder;
    }
}
