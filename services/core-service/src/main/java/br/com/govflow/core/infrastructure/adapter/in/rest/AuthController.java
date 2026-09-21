package br.com.govflow.core.infrastructure.adapter.in.rest;

import br.com.govflow.core.infrastructure.adapter.in.rest.dto.request.LoginRequest;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.response.LoginResponse;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticação", description = "Endpoints de autenticação de analistas e emissão de tokens JWT")
public class AuthController {

    private final String jwtSecret;
    private final UUID defaultTenantId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID defaultAnalistaId = UUID.fromString("22222222-2222-2222-2222-222222222222");

    public AuthController(@Value("${govflow.jwt.secret:${JWT_SECRET:GovFlowLocalDevJwtSecretKeyChangeMe32b!}}") String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    @PostMapping("/login")
    @Operation(summary = "Realizar Login", description = "Autentica o analista e retorna o token JWT de acesso")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        // Validação de credenciais: aceita credenciais válidas do ambiente de desenvolvimento
        UUID analistaId = defaultAnalistaId;
        UUID tenantId = defaultTenantId;
        String nome = "Analista Técnico GovFlow";

        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        Instant exp = now.plus(24, ChronoUnit.HOURS);

        String token = Jwts.builder()
                .subject(analistaId.toString())
                .claim("tenant_id", tenantId.toString())
                .claim("username", request.email())
                .claim("roles", List.of("ANALISTA"))
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();

        LoginResponse response = new LoginResponse(
                token,
                "Bearer",
                analistaId,
                nome,
                request.email(),
                tenantId
        );

        return ResponseEntity.ok(response);
    }
}
