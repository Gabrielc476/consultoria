package br.com.govflow.core.infrastructure.adapter.in.rest.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Registro de alteração individual de um campo no diff")
public record AlteracaoCampoResponse(
        @Schema(description = "Nome do campo fiscal", example = "numeroDocumento")
        String campo,
        @Schema(description = "Valor original sugerido pela IA", example = "000123")
        String de,
        @Schema(description = "Valor corrigido pelo analista", example = "000124")
        String para
) {
}
