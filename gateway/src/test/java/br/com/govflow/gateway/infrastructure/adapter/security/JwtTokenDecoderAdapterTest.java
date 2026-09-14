package br.com.govflow.gateway.infrastructure.adapter.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.govflow.gateway.domain.exception.InvalidTokenException;
import br.com.govflow.gateway.domain.model.UserAuthentication;
import br.com.govflow.gateway.infrastructure.config.GovflowProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class JwtTokenDecoderAdapterTest {

    private static final String SECRET = "GovFlowLocalDevJwtSecretKeyChangeMe32b!";
    private JwtTokenDecoderAdapter adapter;
    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        GovflowProperties props = new GovflowProperties();
        props.getJwt().setSecret(SECRET);
        this.adapter = new JwtTokenDecoderAdapter(props);
        this.secretKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void validTokenIsDecodedSuccessfully() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        String token = Jwts.builder()
                .subject(userId.toString())
                .claim("tenant_id", tenantId.toString())
                .claim("username", "gabriel")
                .claim("roles", List.of("ADMIN", "ANALISTA"))
                .claim("tenant_code", "CONS-01")
                .claim("tenant_active", true)
                .expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(secretKey)
                .compact();

        StepVerifier.create(adapter.decode(token))
                .assertNext(auth -> {
                    assertEquals(userId, auth.userId());
                    assertEquals(tenantId, auth.tenant().tenantId());
                    assertEquals("gabriel", auth.username());
                    assertTrue(auth.hasRole("ADMIN"));
                    assertTrue(auth.hasRole("ANALISTA"));
                    assertTrue(auth.tenant().active());
                })
                .verifyComplete();
    }

    @Test
    void nonUuidSubjectThrowsInvalidTokenException() {
        UUID tenantId = UUID.randomUUID();

        String token = Jwts.builder()
                .subject("non-uuid-username-login")
                .claim("tenant_id", tenantId.toString())
                .expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(secretKey)
                .compact();

        StepVerifier.create(adapter.decode(token))
                .expectErrorMatches(throwable -> throwable instanceof InvalidTokenException
                        && throwable.getMessage().contains("deve ser um UUID válido"))
                .verify();
    }

    @Test
    void missingTenantIdThrowsInvalidTokenException() {
        UUID userId = UUID.randomUUID();

        String token = Jwts.builder()
                .subject(userId.toString())
                .expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(secretKey)
                .compact();

        StepVerifier.create(adapter.decode(token))
                .expectErrorMatches(throwable -> throwable instanceof InvalidTokenException
                        && throwable.getMessage().contains("tenant_id ausente"))
                .verify();
    }

    @Test
    void shortSecretKeyThrowsIllegalStateException() {
        GovflowProperties shortProps = new GovflowProperties();
        shortProps.getJwt().setSecret("too-short");

        assertThrows(IllegalStateException.class, () -> new JwtTokenDecoderAdapter(shortProps));
    }
}
