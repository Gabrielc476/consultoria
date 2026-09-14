package br.com.govflow.gateway.domain.exception;

public class TenantSuspendedException extends RuntimeException {

    public TenantSuspendedException(String message) {
        super(message);
    }
}
