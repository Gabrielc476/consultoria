package br.com.govflow.core.infrastructure.adapter.in.rest.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AprovarCondicionanteRequest(
        @NotBlank(message = "O número do documento comprobatório aprovado é obrigatório (ex: LAE/SPA, Licença ou Matrícula).")
        String numeroDocumentoComprobatorio,

        LocalDate dataAprovacao,

        LocalDate dataValidade,

        @DecimalMin(value = "0.0", message = "O valor do orçamento aprovado deve ser maior ou igual a zero.")
        BigDecimal valorOrcamentoAprovado,

        @DecimalMin(value = "0.0", message = "O percentual de BDI não pode ser negativo.")
        BigDecimal percentualBdiAprovado,

        String numeroArtRrt,

        String orgaoEmissor,

        String s3KeyDocumento
) {
}
