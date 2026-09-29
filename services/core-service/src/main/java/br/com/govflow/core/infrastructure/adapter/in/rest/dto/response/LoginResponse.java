package br.com.govflow.core.infrastructure.adapter.in.rest.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

public record LoginResponse(
        @Schema(description = "Token JWT assinado para autenticação nas rotas protegidas")
        String token,

        @Schema(description = "Tipo do token", example = "Bearer")
        String tokenType,

        @Schema(description = "Identificador único do analista / usuário")
        UUID analistaId,

        @Schema(description = "Nome do analista / usuário", example = "Carlos Gestor")
        String nome,

        @Schema(description = "E-mail de autenticação", example = "gestor@planejabrasil.com.br")
        String email,

        @Schema(description = "Identificador do Tenant (Consultoria)")
        UUID tenantId,

        @Schema(description = "Nome da Consultoria / Razão Social", example = "Planeja Brasil")
        String nomeConsultoria,

        @Schema(description = "Papel do usuário no sistema", example = "ADMIN")
        String role,

        @Schema(description = "Lista de prefeituras atribuídas ao usuário")
        Set<UUID> prefeiturasAtribuidasIds
) {
    public LoginResponse(String token, String tokenType, UUID analistaId, String nome, String email, UUID tenantId, String nomeConsultoria) {
        this(token, tokenType, analistaId, nome, email, tenantId, nomeConsultoria, "ADMIN", Collections.emptySet());
    }

    public LoginResponse(String token, String tokenType, UUID analistaId, String nome, String email, UUID tenantId) {
        this(token, tokenType, analistaId, nome, email, tenantId, "GovFlow Consultoria", "ADMIN", Collections.emptySet());
    }
}
