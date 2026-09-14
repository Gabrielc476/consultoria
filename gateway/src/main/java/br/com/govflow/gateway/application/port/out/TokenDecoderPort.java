package br.com.govflow.gateway.application.port.out;

import br.com.govflow.gateway.domain.model.UserAuthentication;
import reactor.core.publisher.Mono;

public interface TokenDecoderPort {

    Mono<UserAuthentication> decode(String bearerToken);
}
