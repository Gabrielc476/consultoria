package br.com.govflow.core.infrastructure.adapter.in.rest.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Payload para aprovação auditada de documento")
public record AprovarDocumentoRequest(
        @NotNull(message = "O ID do analista é obrigatório para garantir a trilha de auditoria e não-repúdio.")
        @Schema(description = "ID do analista responsável pela aprovação", example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID analistaId,

        @NotNull(message = "Dados da revisão fiscal são obrigatórios.")
        @Valid
        @Schema(description = "Dados finais revisados da nota fiscal")
        DadosRevisaoRequest revisao,

        @Schema(description = "Justificativa ou parecer final do analista", example = "Nota fiscal atestada e retencoes calculadas corretamente")
        String observacao
) {
}
