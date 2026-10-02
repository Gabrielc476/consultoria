package br.com.govflow.core.infrastructure.adapter.in.rest.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DocumentoFicheiroResponse(
        UUID id,
        UUID convenioId,
        UUID prefeituraId,
        String faseCicloVida,
        String faseDescricao,
        String categoriaDocumento,
        String categoriaDescricao,
        String pastaVirtual,
        String nomeArquivoOriginal,
        String contentType,
        Long tamanhoBytes,
        String hashSha256,
        String status,
        String origemCanal,
        List<String> tags,
        UUID criadoPorUsuarioId,
        Instant createdAt,
        Instant updatedAt
) {}
