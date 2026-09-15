package br.com.govflow.core.domain.exception;

public class CodigoIbgeInvalidoException extends DomainException {

    public CodigoIbgeInvalidoException(String message) {
        super("CODIGO_IBGE_INVALIDO", message);
    }
}
