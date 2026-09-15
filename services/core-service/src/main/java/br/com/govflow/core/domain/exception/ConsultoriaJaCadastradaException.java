package br.com.govflow.core.domain.exception;

public class ConsultoriaJaCadastradaException extends DomainException {

    public ConsultoriaJaCadastradaException(String message) {
        super("CONSULTORIA_JA_CADASTRADA", message);
    }
}
