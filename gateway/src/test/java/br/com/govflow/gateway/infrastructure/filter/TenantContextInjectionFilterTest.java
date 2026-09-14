package br.com.govflow.gateway.infrastructure.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import br.com.govflow.gateway.domain.model.TenantContext;
import br.com.govflow.gateway.domain.model.UserAuthentication;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class TenantContextInjectionFilterTest {

    private TenantContextInjectionFilter filter;

    @BeforeEach
    void setUp() {
        this.filter = new TenantContextInjectionFilter();
    }

    @Test
    void unauthenticatedRequestStripsSpoofedHeaders() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/auth/login")
                .header(GatewayHeaders.TENANT_ID, "forged-tenant")
                .header(GatewayHeaders.USER_ID, "forged-user")
                .header(GatewayHeaders.USER_ROLES, "ADMIN")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = mutatedExchange -> {
            assertNull(mutatedExchange.getRequest().getHeaders().getFirst(GatewayHeaders.TENANT_ID));
            assertNull(mutatedExchange.getRequest().getHeaders().getFirst(GatewayHeaders.USER_ID));
            assertNull(mutatedExchange.getRequest().getHeaders().getFirst(GatewayHeaders.USER_ROLES));
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();
    }

    @Test
    void authenticatedRequestInjectsGenuineHeadersAndStripsSpoofed() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserAuthentication auth = new UserAuthentication(
                userId,
                "analista",
                Set.of("ANALISTA", "FINANCEIRO"),
                new TenantContext(tenantId, "CONS-01", true));

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/core/prefeituras")
                .header(GatewayHeaders.TENANT_ID, "forged-tenant")
                .header(GatewayHeaders.USER_ID, "forged-user")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getAttributes().put(GatewayAttributes.USER_AUTHENTICATION, auth);

        GatewayFilterChain chain = mutatedExchange -> {
            assertEquals(tenantId.toString(), mutatedExchange.getRequest().getHeaders().getFirst(GatewayHeaders.TENANT_ID));
            assertEquals(userId.toString(), mutatedExchange.getRequest().getHeaders().getFirst(GatewayHeaders.USER_ID));
            assertEquals("ANALISTA,FINANCEIRO", mutatedExchange.getRequest().getHeaders().getFirst(GatewayHeaders.USER_ROLES));
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();
    }
}
