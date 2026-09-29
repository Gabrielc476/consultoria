package br.com.govflow.core.domain.exception;

public class EmailJaCadastradoException extends DomainException {

    public EmailJaCadastradoException(String email) {
        super("EMAIL_JA_CADASTRADO", "O e-mail " + email + " já está cadastrado no sistema.");
    }
}
