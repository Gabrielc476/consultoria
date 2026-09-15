package br.com.govflow.core.domain.exception;

public class TenantInvalidoException extends DomainException {

    public TenantInvalidoException(String message) {
        super("TENANT_INVALIDO", message);
    }
}
