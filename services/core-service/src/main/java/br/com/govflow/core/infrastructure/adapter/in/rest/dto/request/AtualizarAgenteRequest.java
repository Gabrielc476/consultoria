package br.com.govflow.core.infrastructure.adapter.in.rest.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

public record AtualizarAgenteRequest(
        @Size(max = 150, message = "Nome não pode ultrapassar 150 caracteres.")
        @Schema(description = "Nome do agente", example = "João Analista")
        String nome,

        @Schema(description = "Telefone celular para WhatsApp e alertas", example = "+5583988881111")
        String telefoneCelular,

        @Schema(description = "Status de ativação do agente", example = "true")
        Boolean ativo,

        @Schema(description = "Lista atualizada de prefeituras atribuídas")
        Set<UUID> prefeiturasIds
) {
}
