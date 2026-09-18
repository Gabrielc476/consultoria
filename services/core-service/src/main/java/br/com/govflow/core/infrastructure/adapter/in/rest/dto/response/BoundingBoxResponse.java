package br.com.govflow.core.infrastructure.adapter.in.rest.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Coordenadas retangulares normalizadas (0.0 a 1.0) para highlight no PDF")
public record BoundingBoxResponse(
        @Schema(description = "Y mínimo normalizado", example = "0.152")
        double ymin,
        @Schema(description = "X mínimo normalizado", example = "0.085")
        double xmin,
        @Schema(description = "Y máximo normalizado", example = "0.185")
        double ymax,
        @Schema(description = "X máximo normalizado", example = "0.420")
        double xmax
) {
}
