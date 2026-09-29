package br.com.govflow.core.application.port.in;

import br.com.govflow.core.domain.model.usuario.RoleUsuario;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface GerenciarAgenteUseCase {

    record CriarAgenteCommand(
            String nome,
            String email,
            String senha,
            String telefoneCelular,
            Set<UUID> prefeiturasIds
    ) {
    }

    record AtualizarAgenteCommand(
            UUID id,
            String nome,
            String telefoneCelular,
            Boolean ativo,
            Set<UUID> prefeiturasIds
    ) {
    }

    record AgenteResponse(
            UUID id,
            UUID tenantId,
            String nome,
            String email,
            String telefoneCelular,
            RoleUsuario role,
            boolean ativo,
            Set<UUID> prefeiturasAtribuidasIds,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    AgenteResponse criar(CriarAgenteCommand command);

    List<AgenteResponse> listar();

    AgenteResponse buscarPorId(UUID id);

    AgenteResponse atualizar(AtualizarAgenteCommand command);

    void inativar(UUID id);
}
