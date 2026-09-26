package br.com.govflow.core.domain.exception;

public class DataValidadeInvalidaException extends DomainException {

    public DataValidadeInvalidaException(String message) {
        super("DATA_VALIDADE_INVALIDA", message);
    }
}
