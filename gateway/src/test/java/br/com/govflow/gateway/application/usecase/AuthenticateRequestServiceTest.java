package br.com.govflow.gateway.application.usecase;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import br.com.govflow.gateway.application.port.out.TokenDecoderPort;
import br.com.govflow.gateway.domain.exception.InvalidTokenException;
import br.com.govflow.gateway.domain.model.TenantContext;
import br.com.govflow.gateway.domain.model.UserAuthentication;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class AuthenticateRequestServiceTest {

    @Mock
    private TokenDecoderPort tokenDecoderPort;

    private AuthenticateRequestService service;

    @BeforeEach
    void setUp() {
        service = new AuthenticateRequestService(tokenDecoderPort);
    }

    @Test
    void missingTokenFails() {
        StepVerifier.create(service.execute(null))
                .expectError(InvalidTokenException.class)
                .verify();
    }

    @Test
    void invalidPrefixFails() {
        StepVerifier.create(service.execute("Basic abc123"))
                .expectError(InvalidTokenException.class)
                .verify();
    }

    @Test
    void emptyBearerTokenFails() {
        StepVerifier.create(service.execute("Bearer   "))
                .expectError(InvalidTokenException.class)
                .verify();
    }

    @Test
    void validBearerTokenSucceeds() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserAuthentication auth = new UserAuthentication(
                userId,
                "analista",
                Set.of("ANALISTA"),
                new TenantContext(tenantId, "CONS-01", true));

        when(tokenDecoderPort.decode(anyString())).thenReturn(Mono.just(auth));

        StepVerifier.create(service.execute("Bearer valid.token.here"))
                .expectNext(auth)
                .verifyComplete();
    }
}
