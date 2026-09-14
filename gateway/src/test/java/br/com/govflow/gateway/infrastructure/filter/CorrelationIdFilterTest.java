package br.com.govflow.gateway.infrastructure.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class CorrelationIdFilterTest {

    private CorrelationIdFilter filter;

    @BeforeEach
    void setUp() {
        this.filter = new CorrelationIdFilter();
    }

    @Test
    void missingCorrelationIdGeneratesValidUuid() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/core/status").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = mutatedExchange -> {
            String downstreamCorrId = mutatedExchange.getRequest().getHeaders().getFirst(GatewayHeaders.CORRELATION_ID);
            assertNotNull(downstreamCorrId);
            // Verify it parses as UUID
            UUID.fromString(downstreamCorrId);
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        String responseCorrId = exchange.getResponse().getHeaders().getFirst(GatewayHeaders.CORRELATION_ID);
        assertNotNull(responseCorrId);
        UUID.fromString(responseCorrId);
    }

    @Test
    void safeCorrelationIdIsPreserved() {
        String safeId = "corr-test-123";
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/core/status")
                .header(GatewayHeaders.CORRELATION_ID, safeId)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = mutatedExchange -> {
            String downstreamCorrId = mutatedExchange.getRequest().getHeaders().getFirst(GatewayHeaders.CORRELATION_ID);
            assertEquals(safeId, downstreamCorrId);
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertEquals(safeId, exchange.getResponse().getHeaders().getFirst(GatewayHeaders.CORRELATION_ID));
    }

    @Test
    void unsafeInjectionCorrelationIdIsReplacedWithUuid() {
        String maliciousId = "header\r\nInjected-Header: value";
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/core/status")
                .header(GatewayHeaders.CORRELATION_ID, maliciousId)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = mutatedExchange -> {
            String downstreamCorrId = mutatedExchange.getRequest().getHeaders().getFirst(GatewayHeaders.CORRELATION_ID);
            assertNotEquals(maliciousId, downstreamCorrId);
            UUID.fromString(downstreamCorrId);
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        String responseCorrId = exchange.getResponse().getHeaders().getFirst(GatewayHeaders.CORRELATION_ID);
        assertNotEquals(maliciousId, responseCorrId);
        UUID.fromString(responseCorrId);
    }
}
