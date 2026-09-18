package br.com.govflow.core.infrastructure.adapter.in.rest.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Schema(description = "Registro de auditoria com diff de revisão humana")
public record AuditoriaRevisaoResponse(
        UUID id,
        UUID tenantId,
        UUID documentoId,
        UUID analistaId,
        String acao,
        Instant dataRevisao,
        String justificativa,
        Map<String, AlteracaoCampoResponse> diff,
        Map<String, Object> valoresOriginais,
        Map<String, Object> valoresRevisados
) {
}
