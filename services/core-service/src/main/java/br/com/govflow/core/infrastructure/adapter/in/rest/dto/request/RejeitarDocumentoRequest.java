package br.com.govflow.core.infrastructure.adapter.in.rest.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Payload para rejeição fundamentada de documento")
public record RejeitarDocumentoRequest(
        @NotNull(message = "O ID do analista é obrigatório para garantir a trilha de auditoria e não-repúdio.")
        @Schema(description = "ID do analista responsável pela rejeição", example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID analistaId,

        @NotBlank(message = "O motivo da rejeição é estritamente obrigatório.")
        @Schema(description = "Motivo circunstanciado da rejeição da nota fiscal", example = "Documento ilegível e prestador de serviço não corresponde ao contrato homologado.")
        String motivo
) {
}
