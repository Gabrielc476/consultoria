package br.com.govflow.gateway.application.port.in;

import br.com.govflow.gateway.domain.model.UserAuthentication;
import reactor.core.publisher.Mono;

public interface AuthenticateRequestUseCase {

    /**
     * Authenticates a request. For public paths returns empty Mono (no user context).
     * For protected paths returns authenticated user or error.
     */
    Mono<UserAuthentication> execute(String authHeader, String path);
}
