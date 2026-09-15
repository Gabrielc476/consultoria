package br.com.govflow.gateway.infrastructure.filter;

import br.com.govflow.gateway.domain.model.UserAuthentication;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class TenantContextInjectionFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        UserAuthentication userAuth = exchange.getAttribute(GatewayAttributes.USER_AUTHENTICATION);

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(GatewayHeaders.TENANT_ID);
                    headers.remove(GatewayHeaders.USER_ID);
                    headers.remove(GatewayHeaders.USER_ROLES);

                    if (userAuth != null) {
                        headers.set(GatewayHeaders.TENANT_ID, userAuth.tenant().tenantId().toString());
                        headers.set(GatewayHeaders.USER_ID, userAuth.userId().toString());
                        if (userAuth.roles() != null && !userAuth.roles().isEmpty()) {
                            headers.set(GatewayHeaders.USER_ROLES, String.join(",", userAuth.roles().stream().sorted().toList()));
                        }
                    }
                })
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        return -90;
    }
}
