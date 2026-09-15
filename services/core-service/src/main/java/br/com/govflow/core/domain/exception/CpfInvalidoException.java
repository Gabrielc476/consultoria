package br.com.govflow.core.domain.exception;

public class CpfInvalidoException extends DomainException {

    public CpfInvalidoException(String message) {
        super("CPF_INVALIDO", message);
    }
}
