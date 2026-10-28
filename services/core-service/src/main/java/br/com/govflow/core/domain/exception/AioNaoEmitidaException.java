package br.com.govflow.core.domain.exception;

import java.util.UUID;

public class AioNaoEmitidaException extends DomainException {

    public AioNaoEmitidaException(UUID licitacaoId) {
        super(
                "AIO_NAO_EMITIDA",
                String.format(
                        "TravaAntigrosa: a licitação %s não possui AIO emitida. Boletim de Medição bloqueado até a Autorização de Início de Objeto.",
                        licitacaoId
                )
        );
    }
}
