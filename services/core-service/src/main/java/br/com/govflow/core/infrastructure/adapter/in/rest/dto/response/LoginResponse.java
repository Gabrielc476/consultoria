package br.com.govflow.core.infrastructure.adapter.in.rest.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record LoginResponse(
        @Schema(description = "Token JWT assinado para autenticação nas rotas protegidas")
        String token,

        @Schema(description = "Tipo do token", example = "Bearer")
        String tokenType,

        @Schema(description = "Identificador único do analista")
        UUID analistaId,

        @Schema(description = "Nome do analista", example = "Analista de Convênios")
        String nome,

        @Schema(description = "E-mail de autenticação", example = "analista@govflow.com.br")
        String email,

        @Schema(description = "Identificador do Tenant (Consultoria)")
        UUID tenantId
) {
}
