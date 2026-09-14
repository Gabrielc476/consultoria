package br.com.govflow.gateway.infrastructure.adapter.security;

import br.com.govflow.gateway.application.port.out.TokenDecoderPort;
import br.com.govflow.gateway.domain.exception.InvalidTokenException;
import br.com.govflow.gateway.domain.model.TenantContext;
import br.com.govflow.gateway.domain.model.UserAuthentication;
import br.com.govflow.gateway.infrastructure.config.GovflowProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class JwtTokenDecoderAdapter implements TokenDecoderPort {

    private final SecretKey secretKey;

    public JwtTokenDecoderAdapter(GovflowProperties properties) {
        byte[] keyBytes = properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 bytes for HS256.");
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    public Mono<UserAuthentication> decode(String bearerToken) {
        return Mono.fromCallable(() -> parse(bearerToken))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private UserAuthentication parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String tenantIdRaw = claims.get("tenant_id", String.class);
            if (tenantIdRaw == null || tenantIdRaw.isBlank()) {
                throw new InvalidTokenException("Claim tenant_id ausente no token JWT.");
            }

            UUID tenantId = UUID.fromString(tenantIdRaw);
            UUID userId = resolveUserId(claims);
            String username = claims.get("username", String.class);
            if (username == null) {
                username = claims.getSubject();
            }

            Set<String> roles = resolveRoles(claims);
            Boolean active = claims.get("tenant_active", Boolean.class);
            boolean tenantActive = active == null || active;

            String tenantCode = claims.get("tenant_code", String.class);
            TenantContext tenant = new TenantContext(tenantId, tenantCode, tenantActive);
            return new UserAuthentication(userId, username, roles, tenant);
        } catch (InvalidTokenException ex) {
            throw ex;
        } catch (IllegalArgumentException ex) {
            throw new InvalidTokenException("Claim UUID inválida no token JWT.", ex);
        } catch (JwtException ex) {
            throw new InvalidTokenException("O token fornecido expirou ou não possui assinatura válida.", ex);
        }
    }

    private UUID resolveUserId(Claims claims) {
        String userIdClaim = claims.get("user_id", String.class);
        if (userIdClaim != null && !userIdClaim.isBlank()) {
            try {
                return UUID.fromString(userIdClaim);
            } catch (IllegalArgumentException ex) {
                throw new InvalidTokenException("Claim user_id deve ser um UUID válido.", ex);
            }
        }
        String sub = claims.getSubject();
        if (sub == null || sub.isBlank()) {
            throw new InvalidTokenException("Claim sub/user_id ausente no token JWT.");
        }
        try {
            return UUID.fromString(sub);
        } catch (IllegalArgumentException ex) {
            throw new InvalidTokenException("Claim sub deve ser um UUID válido.", ex);
        }
    }

    private Set<String> resolveRoles(Claims claims) {
        Object rolesClaim = claims.get("roles");
        if (rolesClaim instanceof String rolesStr) {
            if (rolesStr.isBlank()) {
                return Set.of();
            }
            return Arrays.stream(rolesStr.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        if (rolesClaim instanceof Iterable<?> iterable) {
            Set<String> roles = new LinkedHashSet<>();
            for (Object item : iterable) {
                if (item != null) {
                    roles.add(item.toString());
                }
            }
            return roles;
        }
        return Set.of();
    }
}
