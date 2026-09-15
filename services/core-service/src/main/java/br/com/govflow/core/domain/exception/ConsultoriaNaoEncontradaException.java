package br.com.govflow.core.domain.exception;

import java.util.UUID;

public class ConsultoriaNaoEncontradaException extends DomainException {

    public ConsultoriaNaoEncontradaException(UUID id) {
        super("CONSULTORIA_NAO_ENCONTRADA", "Consultoria com ID " + id + " não foi encontrada.");
    }
}
