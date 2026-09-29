package br.com.govflow.core.application.port.in;

import br.com.govflow.core.application.port.in.AutenticarUsuarioUseCase.UsuarioAutenticado;

import java.util.UUID;

public interface ObterUsuarioAutenticadoUseCase {

    UsuarioAutenticado obterPerfil(UUID usuarioId, UUID tenantId);
}
