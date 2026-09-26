package br.com.govflow.core.infrastructure.adapter.in.rest.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record RegistrarDiligenciaRequest(
        @NotBlank(message = "As observações da diligência da Caixa são obrigatórias.")
        String observacoes,

        String s3KeyLaudoPendencias,

        @NotNull(message = "A data limite de saneamento é obrigatória.")
        @Future(message = "A data limite de saneamento deve estar no futuro.")
        LocalDate dataLimiteSaneamento
) {
}
