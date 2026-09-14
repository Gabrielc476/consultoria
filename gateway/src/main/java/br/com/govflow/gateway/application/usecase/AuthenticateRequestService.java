package br.com.govflow.gateway.application.usecase;

import br.com.govflow.gateway.application.port.in.AuthenticateRequestUseCase;
import br.com.govflow.gateway.application.port.out.TokenDecoderPort;
import br.com.govflow.gateway.domain.exception.InvalidTokenException;
import br.com.govflow.gateway.domain.model.RoutePolicy;
import br.com.govflow.gateway.domain.model.UserAuthentication;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class AuthenticateRequestService implements AuthenticateRequestUseCase {

    private final TokenDecoderPort tokenDecoderPort;

    public AuthenticateRequestService(TokenDecoderPort tokenDecoderPort) {
        this.tokenDecoderPort = tokenDecoderPort;
    }

    @Override
    public Mono<UserAuthentication> execute(String authHeader, String path) {
        if (RoutePolicy.isPublicPath(path)) {
            return Mono.empty();
        }

        if (authHeader == null || authHeader.isBlank()) {
            return Mono.error(new InvalidTokenException("Token JWT ausente. Informe o header Authorization: Bearer <token>."));
        }

        if (!authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return Mono.error(new InvalidTokenException("Formato de Authorization inválido. Use Bearer <token>."));
        }

        String token = authHeader.substring(7).trim();
        if (token.isEmpty()) {
            return Mono.error(new InvalidTokenException("Token JWT ausente após o prefixo Bearer."));
        }

        return tokenDecoderPort.decode(token)
                .doOnNext(auth -> auth.tenant().validateActive());
    }
}
