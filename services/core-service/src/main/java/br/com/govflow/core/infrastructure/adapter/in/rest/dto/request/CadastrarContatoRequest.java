package br.com.govflow.core.infrastructure.adapter.in.rest.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.UUID;

public record CadastrarContatoRequest(
        @NotBlank(message = "Nome é obrigatório")
        String nome,

        @NotBlank(message = "Número de telefone é obrigatório")
        String phoneNumber,

        String papel,
        String empresaOuOrgao,
        List<UUID> conveniosIds,
        UUID convenioPrincipalId
) {
}
