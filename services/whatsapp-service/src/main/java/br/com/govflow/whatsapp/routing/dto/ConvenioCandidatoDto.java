package br.com.govflow.whatsapp.routing.dto;

import java.util.UUID;

public record ConvenioCandidatoDto(
        UUID convenioId,
        UUID prefeituraId,
        String papelEspecifico,
        boolean principal
) {}
