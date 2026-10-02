package br.com.govflow.core.infrastructure.adapter.in.rest.dto.response;

import java.util.UUID;

public record ContatoConvenioResponse(
        UUID convenioId,
        UUID prefeituraId,
        String numeroSiconv,
        String objeto,
        String papelEspecifico,
        boolean principal
) {
}
