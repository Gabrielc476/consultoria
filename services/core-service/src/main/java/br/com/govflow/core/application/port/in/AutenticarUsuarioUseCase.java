package br.com.govflow.core.application.port.in;

import br.com.govflow.core.domain.model.usuario.RoleUsuario;

import java.util.Set;
import java.util.UUID;

public interface AutenticarUsuarioUseCase {

    record AutenticarUsuarioCommand(
            String email,
            String senha
    ) {
    }

    record UsuarioAutenticado(
            String token,
            String tokenType,
            UUID id,
            String nome,
            String email,
            UUID tenantId,
            String nomeConsultoria,
            RoleUsuario role,
            Set<UUID> prefeiturasAtribuidasIds
    ) {
    }

    UsuarioAutenticado autenticar(AutenticarUsuarioCommand command);
}
