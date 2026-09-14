package br.com.govflow.gateway.infrastructure.filter;

import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    private static final Pattern SAFE_CORRELATION_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{1,64}$");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String incoming = exchange.getRequest().getHeaders().getFirst(GatewayHeaders.CORRELATION_ID);
        String correlationId = resolveCorrelationId(incoming);

        ServerHttpRequest mutated = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(GatewayHeaders.CORRELATION_ID);
                    headers.set(GatewayHeaders.CORRELATION_ID, correlationId);
                })
                .build();

        exchange.getResponse().getHeaders().set(GatewayHeaders.CORRELATION_ID, correlationId);
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    private static String resolveCorrelationId(String incoming) {
        if (incoming != null) {
            String trimmed = incoming.trim();
            if (SAFE_CORRELATION_ID_PATTERN.matcher(trimmed).matches()) {
                return trimmed;
            }
        }
        return UUID.randomUUID().toString();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
