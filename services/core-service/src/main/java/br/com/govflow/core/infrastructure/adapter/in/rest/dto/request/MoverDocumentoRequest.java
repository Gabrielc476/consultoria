package br.com.govflow.core.infrastructure.adapter.in.rest.dto.request;

import jakarta.validation.constraints.NotBlank;

public record MoverDocumentoRequest(
        @NotBlank(message = "A nova fase de destino é obrigatória.")
        String novaFase,
        String novaPastaVirtual,
        String justificativa
) {}
