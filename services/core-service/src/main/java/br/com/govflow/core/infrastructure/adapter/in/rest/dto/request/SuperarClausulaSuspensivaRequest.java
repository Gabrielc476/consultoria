package br.com.govflow.core.infrastructure.adapter.in.rest.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SuperarClausulaSuspensivaRequest(
        @NotBlank(message = "A chave do Termo de Retirada da Cláusula Suspensiva é obrigatória.")
        String s3KeyTermoRetirada
) {
}
