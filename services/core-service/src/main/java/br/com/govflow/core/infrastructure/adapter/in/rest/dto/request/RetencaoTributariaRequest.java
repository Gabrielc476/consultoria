package br.com.govflow.core.infrastructure.adapter.in.rest.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

@Schema(description = "Item de dedução ou retenção tributária (INSS, ISS, IRRF, etc.)")
public record RetencaoTributariaRequest(
        @NotBlank(message = "O tipo do tributo retido é obrigatório (ex: INSS, ISS, IRRF).")
        @Schema(description = "Tipo do imposto", example = "INSS")
        @JsonAlias("tipoTributo")
        String tipo,

        @PositiveOrZero(message = "A alíquota não pode ser negativa.")
        @Schema(description = "Alíquota aplicada em percentual", example = "11.00")
        @JsonAlias("aliquotaPercentual")
        BigDecimal aliquota,

        @NotNull(message = "O valor retido é obrigatório.")
        @PositiveOrZero(message = "O valor retido não pode ser negativo.")
        @Schema(description = "Valor absoluto retido", example = "1100.00")
        @JsonAlias("valorRetido")
        BigDecimal valor
) {
}
