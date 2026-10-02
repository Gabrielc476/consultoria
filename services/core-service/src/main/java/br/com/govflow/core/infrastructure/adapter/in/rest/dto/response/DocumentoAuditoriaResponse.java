package br.com.govflow.core.infrastructure.adapter.in.rest.dto.response;

import java.time.Instant;
import java.util.UUID;

public record DocumentoAuditoriaResponse(
        UUID id,
        UUID documentoId,
        UUID usuarioId,
        String acao,
        String justificativa,
        String estadoAnterior,
        String estadoNovo,
        Instant createdAt
) {
}
