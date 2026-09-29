package br.com.govflow.core.domain.exception;

public class CredenciaisInvalidasException extends DomainException {

    public CredenciaisInvalidasException(String message) {
        super("CREDENCIAIS_INVALIDAS", message);
    }

    public CredenciaisInvalidasException() {
        super("CREDENCIAIS_INVALIDAS", "E-mail ou senha inválidos.");
    }
}
