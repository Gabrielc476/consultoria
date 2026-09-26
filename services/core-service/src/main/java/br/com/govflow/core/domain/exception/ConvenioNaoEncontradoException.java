package br.com.govflow.core.domain.exception;

import java.util.UUID;

public class ConvenioNaoEncontradoException extends DomainException {

    public ConvenioNaoEncontradoException(UUID id) {
        super("CONVENIO_NAO_ENCONTRADO", String.format("Convênio com ID %s não foi encontrado.", id));
    }

    public ConvenioNaoEncontradoException(String numeroSiconv) {
        super("CONVENIO_NAO_ENCONTRADO", String.format("Convênio com número SICONV %s não foi encontrado.", numeroSiconv));
    }
}
