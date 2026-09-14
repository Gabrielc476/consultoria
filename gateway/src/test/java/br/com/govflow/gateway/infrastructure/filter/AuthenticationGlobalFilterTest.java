package br.com.govflow.gateway.infrastructure.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.govflow.gateway.application.port.in.AuthenticateRequestUseCase;
import br.com.govflow.gateway.domain.exception.InvalidTokenException;
import br.com.govflow.gateway.domain.exception.TenantSuspendedException;
import br.com.govflow.gateway.domain.model.TenantContext;
import br.com.govflow.gateway.domain.model.UserAuthentication;
import br.com.govflow.gateway.infrastructure.error.ProblemDetailsWriter;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class AuthenticationGlobalFilterTest {

    @Mock
    private AuthenticateRequestUseCase authenticateRequestUseCase;

    @Mock
    private ProblemDetailsWriter problemDetailsWriter;

    @Mock
    private GatewayFilterChain chain;

    private AuthenticationGlobalFilter filter;

    @BeforeEach
    void setUp() {
        this.filter = new AuthenticationGlobalFilter(authenticateRequestUseCase, problemDetailsWriter);
    }

    @Test
    void publicPathSkipsAuthentication() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/auth/login").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(authenticateRequestUseCase, never()).execute(anyString());
        verify(chain).filter(exchange);
    }

    @Test
    void protectedPathWithValidTokenSetsAttribute() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserAuthentication auth = new UserAuthentication(
                userId,
                "analista",
                Set.of("ANALISTA"),
                new TenantContext(tenantId, "CONS-01", true));

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/core/prefeituras")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid.token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(authenticateRequestUseCase.execute("Bearer valid.token")).thenReturn(Mono.just(auth));
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        UserAuthentication stored = exchange.getAttribute(GatewayAttributes.USER_AUTHENTICATION);
        assertNotNull(stored);
        assertEquals(userId, stored.userId());
        verify(chain).filter(exchange);
    }

    @Test
    void protectedPathWithInvalidTokenWritesProblemDetails401() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/core/prefeituras").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(authenticateRequestUseCase.execute(null))
                .thenReturn(Mono.error(new InvalidTokenException("Token JWT ausente.")));
        when(problemDetailsWriter.write(eq(exchange), eq(HttpStatus.UNAUTHORIZED), anyString(), anyString(), anyString()))
                .thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(problemDetailsWriter).write(
                eq(exchange),
                eq(HttpStatus.UNAUTHORIZED),
                eq("https://govflow.com.br/errors/unauthorized"),
                eq("Token Inválido ou Ausente"),
                eq("Token JWT ausente."));
        verify(chain, never()).filter(exchange);
    }

    @Test
    void protectedPathWithSuspendedTenantWritesProblemDetails403() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/core/prefeituras")
                .header(HttpHeaders.AUTHORIZATION, "Bearer token.suspended")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(authenticateRequestUseCase.execute("Bearer token.suspended"))
                .thenReturn(Mono.error(new TenantSuspendedException("Consultoria com acesso suspenso.")));
        when(problemDetailsWriter.write(eq(exchange), eq(HttpStatus.FORBIDDEN), anyString(), anyString(), anyString()))
                .thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(problemDetailsWriter).write(
                eq(exchange),
                eq(HttpStatus.FORBIDDEN),
                eq("https://govflow.com.br/errors/tenant-suspended"),
                eq("Consultoria Suspensa"),
                eq("Consultoria com acesso suspenso."));
        verify(chain, never()).filter(exchange);
    }
}
