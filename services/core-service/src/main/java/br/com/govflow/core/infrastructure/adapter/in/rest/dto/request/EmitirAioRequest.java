package br.com.govflow.core.infrastructure.adapter.in.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record EmitirAioRequest(
        @NotBlank
        String numeroAio,

        @NotNull
        LocalDate dataEmissaoAio
) {}
