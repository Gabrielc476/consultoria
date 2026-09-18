package br.com.govflow.core.infrastructure.adapter.in.rest.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Schema(description = "Dados consolidados do Documento para visualização e conferência")
public record DocumentoResponse(
        UUID id,
        UUID tenantId,
        UUID prefeituraId,
        UUID convenioId,
        String s3Bucket,
        String s3Key,
        String nomeArquivoOriginal,
        String contentType,
        Long tamanhoBytes,
        String status,
        ExtracaoSugeridaResponse extracaoSugerida,
        Map<String, BoundingBoxResponse> boundingBoxes,
        DadosRevisaoResponse dadosRevisao,
        String motivoRejeicao,
        Instant createdAt,
        Instant updatedAt
) {
}
