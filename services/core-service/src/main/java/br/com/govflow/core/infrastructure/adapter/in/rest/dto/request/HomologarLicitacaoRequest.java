package br.com.govflow.core.infrastructure.adapter.in.rest.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record HomologarLicitacaoRequest(
        @NotNull @DecimalMin(value = "0.01")
        BigDecimal valorHomologado,

        @NotNull
        LocalDate dataHomologacao,

        @NotBlank
        String cnpjVencedor,

        @NotBlank
        String razaoSocialVencedor
) {}
