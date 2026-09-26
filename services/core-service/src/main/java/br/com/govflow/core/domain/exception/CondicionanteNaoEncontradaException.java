package br.com.govflow.core.domain.exception;

import br.com.govflow.core.domain.model.convenio.TipoCondicionanteSuspensiva;

import java.util.UUID;

public class CondicionanteNaoEncontradaException extends DomainException {

    public CondicionanteNaoEncontradaException(UUID id) {
        super("CONDICIONANTE_NAO_ENCONTRADA", String.format("Condicionante suspensiva não encontrada para o id: %s", id));
    }

    public CondicionanteNaoEncontradaException(UUID convenioId, TipoCondicionanteSuspensiva tipo) {
        super("CONDICIONANTE_NAO_ENCONTRADA", String.format("Condicionante suspensiva do tipo '%s' não encontrada para o convênio: %s", tipo, convenioId));
    }
}
