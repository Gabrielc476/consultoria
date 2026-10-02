package br.com.govflow.core.infrastructure.adapter.in.rest.dto.response;

import java.util.List;
import java.util.UUID;

public record FicheiroDigitalResponse(
        UUID convenioId,
        UUID prefeituraId,
        UUID tenantId,
        String numeroSiconv,
        String objeto,
        int totalArquivos,
        long tamanhoTotalBytes,
        List<PastaFaseResponse> fases
) {}
