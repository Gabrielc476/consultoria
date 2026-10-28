package br.com.govflow.core.infrastructure.adapter.in.rest.dto.request;

import br.com.govflow.core.domain.model.licitacao.StatusVrpl;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record RegistrarResultadoVrplRequest(
        @NotNull
        StatusVrpl resultado,

        String parecerTecnico,

        String observacoesDiligencia,

        LocalDate dataAceiteVrpl
) {}
