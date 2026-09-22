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
        if (org.springframework.http.HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())) {
            return chain.filter(exchange);
        }

        String path = exchange.getRequest().getURI().getPath();

        if (RoutePolicy.isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || authHeader.isBlank()) {
            String tokenParam = exchange.getRequest().getQueryParams().getFirst("token");
            if (tokenParam != null && !tokenParam.isBlank()) {
                authHeader = "Bearer " + tokenParam;
            }
        }

        return authenticateRequestUseCase.execute(authHeader)
                .flatMap(userAuth -> {
                    exchange.getAttributes().put(GatewayAttributes.USER_AUTHENTICATION, userAuth);
                    return chain.filter(exchange);
                })
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

    @Override
    public int getOrder() {
        return -100;
    }
}
