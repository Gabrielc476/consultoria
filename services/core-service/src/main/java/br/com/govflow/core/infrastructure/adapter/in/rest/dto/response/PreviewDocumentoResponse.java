package br.com.govflow.core.infrastructure.adapter.in.rest.dto.response;

import java.util.UUID;

public record PreviewDocumentoResponse(
        UUID documentoId,
        String url,
        int expiraEmMinutos
) {}
