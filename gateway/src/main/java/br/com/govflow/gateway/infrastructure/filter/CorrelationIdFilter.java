package br.com.govflow.gateway.infrastructure.filter;

import java.util.UUID;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String incoming = exchange.getRequest().getHeaders().getFirst(GatewayHeaders.CORRELATION_ID);
        String correlationId = (incoming == null || incoming.isBlank())
                ? UUID.randomUUID().toString()
                : incoming.trim();

        ServerHttpRequest mutated = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(GatewayHeaders.CORRELATION_ID);
                    headers.set(GatewayHeaders.CORRELATION_ID, correlationId);
                })
                .build();

        exchange.getResponse().getHeaders().set(GatewayHeaders.CORRELATION_ID, correlationId);
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
