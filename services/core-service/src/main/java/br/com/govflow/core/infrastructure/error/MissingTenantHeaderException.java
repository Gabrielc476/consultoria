package br.com.govflow.core.infrastructure.error;

public class MissingTenantHeaderException extends RuntimeException {

    public MissingTenantHeaderException(String message) {
        super(message);
    }
}
