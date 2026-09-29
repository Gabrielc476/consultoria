package br.com.govflow.core.infrastructure.adapter.in.rest.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

public record CriarAgenteRequest(
        @NotBlank(message = "Nome é obrigatório.")
        @Size(max = 150, message = "Nome não pode ultrapassar 150 caracteres.")
        @Schema(description = "Nome do agente / analista", example = "João Analista")
        String nome,

        @NotBlank(message = "E-mail é obrigatório.")
        @Email(message = "E-mail inválido.")
        @Schema(description = "E-mail de acesso do agente", example = "joao@planejabrasil.com.br")
        String email,

        @NotBlank(message = "Senha é obrigatória.")
        @Size(min = 6, message = "A senha deve ter no mínimo 6 caracteres.")
        @Schema(description = "Senha de acesso temporária do agente", example = "GovFlow2026!")
        String senha,

        @Schema(description = "Número de telefone celular para WhatsApp e alertas", example = "+5583988881111")
        String telefoneCelular,

        @Schema(description = "Lista de IDs das prefeituras vinculadas a este agente")
        Set<UUID> prefeiturasIds
) {
}
