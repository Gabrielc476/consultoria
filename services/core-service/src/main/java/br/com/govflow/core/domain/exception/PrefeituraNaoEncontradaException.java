package br.com.govflow.core.domain.exception;

import java.util.UUID;

public class PrefeituraNaoEncontradaException extends DomainException {

    public PrefeituraNaoEncontradaException(UUID id) {
        super("PREFEITURA_NAO_ENCONTRADA", "Prefeitura com ID " + id + " não foi encontrada para a consultoria informada.");
    }
}
