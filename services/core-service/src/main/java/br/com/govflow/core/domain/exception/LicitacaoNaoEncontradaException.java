package br.com.govflow.core.domain.exception;

import java.util.UUID;

public class LicitacaoNaoEncontradaException extends DomainException {

    public LicitacaoNaoEncontradaException(UUID id) {
        super("LICITACAO_NAO_ENCONTRADA", String.format("Licitação com ID %s não foi encontrada.", id));
    }
}
