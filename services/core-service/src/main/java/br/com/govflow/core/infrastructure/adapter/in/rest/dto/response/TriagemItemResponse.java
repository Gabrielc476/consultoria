package br.com.govflow.core.infrastructure.adapter.in.rest.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TriagemItemResponse(
        UUID id,
        UUID tenantId,
        UUID agenteResponsavelId,
        UUID mensagemInboundId,
        UUID documentoId,
        UUID convenioSugeridoId,
        String convenioSugeridoNumeroSiconv,
        String convenioSugeridoObjeto,
        UUID prefeituraSugeridaId,
        String faseSugerida,
        BigDecimal confidenceScore,
        String motivoAmbiguidade,
        String phoneNumber,
        String senderName,
        String pushName,
        boolean remetenteNovo,
        String conteudoResumo,
        String status,
        Instant resolvidoEm,
        Instant createdAt,
        String documentoNomeOriginal,
        String documentoContentType,
        Long documentoTamanhoBytes
) {
}
