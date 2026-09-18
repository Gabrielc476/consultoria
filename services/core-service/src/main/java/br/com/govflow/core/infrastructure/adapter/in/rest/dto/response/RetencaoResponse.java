package br.com.govflow.core.infrastructure.adapter.in.rest.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Retenção tributária com valores e coordenadas")
public record RetencaoResponse(
        @Schema(description = "Tipo de imposto retido", example = "INSS")
        String tipo,
        @Schema(description = "Alíquota percentual", example = "11.00")
        BigDecimal aliquota,
        @Schema(description = "Valor retido", example = "1100.00")
        BigDecimal valor,
        @Schema(description = "Índice de confiança da IA", example = "0.99")
        double confianca,
        @Schema(description = "Coordenadas da bounding box no documento")
        BoundingBoxResponse coordenadas
) {
}
