package br.com.govflow.core.infrastructure.adapter.in.rest.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record SolicitarProrrogacaoRequest(
        @NotNull(message = "A nova data proposta para prorrogação é obrigatória.")
        @Future(message = "A data prorrogada deve ser futura.")
        LocalDate novoPrazoProrrogado
) {
}
