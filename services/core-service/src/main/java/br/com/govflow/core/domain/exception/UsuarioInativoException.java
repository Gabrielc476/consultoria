package br.com.govflow.core.domain.exception;

public class UsuarioInativoException extends DomainException {

    public UsuarioInativoException(String message) {
        super("USUARIO_INATIVO", message);
    }

    public UsuarioInativoException() {
        super("USUARIO_INATIVO", "Usuário inativo. Entre em contato com o administrador da consultoria.");
    }
}
