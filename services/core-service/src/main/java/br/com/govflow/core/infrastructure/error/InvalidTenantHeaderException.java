package br.com.govflow.core.infrastructure.error;

public class InvalidTenantHeaderException extends RuntimeException {

    public InvalidTenantHeaderException(String message) {
        super(message);
    }
}
