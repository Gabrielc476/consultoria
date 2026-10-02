package br.com.govflow.core.domain.exception;

import java.util.UUID;

public class TriagemItemNaoEncontradoException extends DomainException {

    public TriagemItemNaoEncontradoException(UUID id) {
        super("TRIAGEM_ITEM_NAO_ENCONTRADO", "Item de triagem não encontrado: " + id);
    }
}
