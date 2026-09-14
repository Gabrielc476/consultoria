package br.com.govflow.gateway.application.port.in;

import br.com.govflow.gateway.domain.model.UserAuthentication;
import reactor.core.publisher.Mono;

public interface AuthenticateRequestUseCase {

    /**
     * Authenticates a request based on the authorization header.
     * Returns authenticated user or error.
     */
    Mono<UserAuthentication> execute(String authHeader);
}
