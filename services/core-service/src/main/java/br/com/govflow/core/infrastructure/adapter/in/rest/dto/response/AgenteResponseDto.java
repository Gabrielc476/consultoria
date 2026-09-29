package br.com.govflow.core.infrastructure.adapter.in.rest.dto.response;

import br.com.govflow.core.application.port.in.GerenciarAgenteUseCase.AgenteResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record AgenteResponseDto(
        @Schema(description = "Identificador único do agente")
        UUID id,

        @Schema(description = "Identificador da consultoria (Tenant)")
        UUID tenantId,

        @Schema(description = "Nome do agente", example = "João Analista")
        String nome,

        @Schema(description = "E-mail do agente", example = "joao@planejabrasil.com.br")
        String email,

        @Schema(description = "Telefone celular no padrão E.164 para WhatsApp", example = "+5583988881111")
        String telefoneCelular,

        @Schema(description = "Papel no sistema", example = "AGENTE")
        String role,

        @Schema(description = "Status de ativação", example = "true")
        boolean ativo,

        @Schema(description = "Lista de prefeituras atribuídas a este agente")
        Set<UUID> prefeiturasAtribuidasIds,

        Instant createdAt,
        Instant updatedAt
) {
    public static AgenteResponseDto fromDomain(AgenteResponse response) {
        return new AgenteResponseDto(
                response.id(),
                response.tenantId(),
                response.nome(),
                response.email(),
                response.telefoneCelular(),
                response.role().name(),
                response.ativo(),
                response.prefeiturasAtribuidasIds(),
                response.createdAt(),
                response.updatedAt()
        );
    }
}
