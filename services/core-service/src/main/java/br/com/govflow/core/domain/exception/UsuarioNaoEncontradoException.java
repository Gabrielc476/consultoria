package br.com.govflow.core.domain.exception;

import java.util.UUID;

public class UsuarioNaoEncontradoException extends DomainException {

    public UsuarioNaoEncontradoException(UUID id) {
        super("USUARIO_NAO_ENCONTRADO", "Usuário não encontrado com o identificador: " + id);
    }

    public UsuarioNaoEncontradoException(String email) {
        super("USUARIO_NAO_ENCONTRADO", "Usuário não encontrado com o e-mail: " + email);
    }
}
