package br.com.govflow.core.infrastructure.adapter.in.rest.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Dados fiscais validados/corrigidos pelo analista")
public record DadosRevisaoRequest(
        @NotBlank(message = "Tipo de documento hábil é obrigatório.")
        @Schema(description = "Tipo do documento", example = "NOTA_FISCAL_SERVICOS")
        String tipoDocumento,

        @NotBlank(message = "Número da nota fiscal é obrigatório.")
        @Schema(description = "Número da NF", example = "000123")
        String numeroDocumento,

        @Schema(description = "Série do documento", example = "1")
        String serieDocumento,

        @Schema(description = "Chave de acesso da NF-e (44 dígitos)", example = "35240512345678000190550010000001231000001234")
        String chaveAcessoNfe,

        @NotNull(message = "Data de emissão é obrigatória.")
        @Schema(description = "Data de emissão da NF", example = "2026-05-10")
        LocalDate dataEmissao,

        @NotBlank(message = "CNPJ do credor é obrigatório.")
        @Schema(description = "CNPJ do prestador/fornecedor", example = "12.345.678/0001-90")
        String cnpjCredor,

        @NotBlank(message = "Razão social do credor é obrigatória.")
        @Schema(description = "Razão social", example = "Construtora Progresso Ltda")
        String razaoSocialCredor,

        @Schema(description = "Descrição dos serviços/materiais")
        String descricaoServico,

        @Schema(description = "Número do empenho municipal", example = "2026NE00012")
        String numeroEmpenho,

        @NotNull(message = "Valor bruto é obrigatório.")
        @Positive(message = "Valor bruto deve ser maior que zero.")
        @Schema(description = "Valor bruto total da NF", example = "10000.00")
        BigDecimal valorBruto,

        @PositiveOrZero(message = "Total de deduções não pode ser negativo.")
        @Schema(description = "Soma total das deduções e retenções", example = "1100.00")
        BigDecimal valorTotalDeducoes,

        @NotNull(message = "Valor líquido é obrigatório.")
        @PositiveOrZero(message = "Valor líquido não pode ser negativo.")
        @Schema(description = "Valor líquido a pagar", example = "8900.00")
        BigDecimal valorLiquido,

        @Valid
        @Schema(description = "Lista detalhada de tributos retidos na fonte")
        List<RetencaoTributariaRequest> retencoes,

        @Schema(description = "Observações da conferência", example = "Valores conferidos conforme medição")
        String observacao
) {
}
