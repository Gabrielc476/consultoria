package br.com.govflow.whatsapp.routing.dto;

import java.time.Instant;

public record HistoricoMensagemDto(
        Instant timestamp,
        String tipo,
        String texto,
        String audioTranscription,
        String nomeArquivo
) {}
