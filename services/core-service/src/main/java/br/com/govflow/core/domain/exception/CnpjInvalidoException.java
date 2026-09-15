package br.com.govflow.core.domain.exception;

public class CnpjInvalidoException extends DomainException {

    public CnpjInvalidoException(String message) {
        super("CNPJ_INVALIDO", message);
    }
}
