package br.com.govflow.gateway.infrastructure.filter;

import br.com.govflow.gateway.application.port.in.AuthenticateRequestUseCase;
import br.com.govflow.gateway.domain.exception.InvalidTokenException;
import br.com.govflow.gateway.domain.exception.TenantSuspendedException;
import br.com.govflow.gateway.domain.model.RoutePolicy;
import br.com.govflow.gateway.domain.model.UserAuthentication;
import br.com.govflow.gateway.infrastructure.error.ProblemDetailsWriter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthenticationGlobalFilter implements GlobalFilter, Ordered {

    private final AuthenticateRequestUseCase authenticateRequestUseCase;
    private final ProblemDetailsWriter problemDetailsWriter;

    public AuthenticationGlobalFilter(
            AuthenticateRequestUseCase authenticateRequestUseCase,
            ProblemDetailsWriter problemDetailsWriter) {
        this.authenticateRequestUseCase = authenticateRequestUseCase;
        this.problemDetailsWriter = problemDetailsWriter;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (RoutePolicy.isPublicPath(path)) {
            ServerHttpRequest sanitized = stripSecurityHeaders(exchange.getRequest());
            return chain.filter(exchange.mutate().request(sanitized).build());
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        return authenticateRequestUseCase.execute(authHeader, path)
                .flatMap(userAuth -> chain.filter(exchange.mutate().request(injectHeaders(exchange, userAuth)).build()))
                .onErrorResume(InvalidTokenException.class, ex ->
                        problemDetailsWriter.write(
                                exchange,
                                HttpStatus.UNAUTHORIZED,
                                "https://govflow.com.br/errors/unauthorized",
                                "Token Inválido ou Ausente",
                                ex.getMessage()))
                .onErrorResume(TenantSuspendedException.class, ex ->
                        problemDetailsWriter.write(
                                exchange,
                                HttpStatus.FORBIDDEN,
                                "https://govflow.com.br/errors/tenant-suspended",
                                "Consultoria Suspensa",
                                ex.getMessage()));
    }

    private ServerHttpRequest injectHeaders(ServerWebExchange exchange, UserAuthentication userAuth) {
        return exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(GatewayHeaders.TENANT_ID);
                    headers.remove(GatewayHeaders.USER_ID);
                    headers.remove(GatewayHeaders.USER_ROLES);
                    headers.set(GatewayHeaders.TENANT_ID, userAuth.tenant().tenantId().toString());
                    headers.set(GatewayHeaders.USER_ID, userAuth.userId().toString());
                    if (userAuth.roles() != null && !userAuth.roles().isEmpty()) {
                        headers.set(GatewayHeaders.USER_ROLES, String.join(",", userAuth.roles()));
                    }
                })
                .build();
    }

    private ServerHttpRequest stripSecurityHeaders(ServerHttpRequest request) {
        return request.mutate()
                .headers(headers -> {
                    headers.remove(GatewayHeaders.TENANT_ID);
                    headers.remove(GatewayHeaders.USER_ID);
                    headers.remove(GatewayHeaders.USER_ROLES);
                })
                .build();
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
