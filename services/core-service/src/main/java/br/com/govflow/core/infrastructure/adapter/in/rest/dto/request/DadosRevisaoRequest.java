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

        @NotBlank(message = "Número do documento é obrigatório.")
        @Schema(description = "Número do documento / protocolo / processo", example = "000123")
        String numeroDocumento,

        @Schema(description = "Série do documento", example = "1")
        String serieDocumento,

        @Schema(description = "Chave de acesso da NF-e (44 dígitos)", example = "35240512345678000190550010000001231000001234")
        String chaveAcessoNfe,

        @Schema(description = "Data de emissão", example = "2026-05-10")
        LocalDate dataEmissao,

        @Schema(description = "CNPJ do prestador/fornecedor/emissor", example = "12.345.678/0001-90")
        String cnpjCredor,

        @Schema(description = "Razão social ou órgão emissor", example = "Construtora Progresso Ltda")
        String razaoSocialCredor,

        @Schema(description = "Descrição dos serviços/objeto")
        String descricaoServico,

        @Schema(description = "Número do empenho municipal", example = "2026NE00012")
        String numeroEmpenho,

        @PositiveOrZero(message = "Valor bruto não pode ser negativo.")
        @Schema(description = "Valor bruto total do documento", example = "10000.00")
        BigDecimal valorBruto,

        @PositiveOrZero(message = "Total de deduções não pode ser negativo.")
        @Schema(description = "Soma total das deduções e retenções", example = "1100.00")
        BigDecimal valorTotalDeducoes,

        @PositiveOrZero(message = "Valor líquido não pode ser negativo.")
        @Schema(description = "Valor líquido", example = "8900.00")
        BigDecimal valorLiquido,

        @Valid
        @Schema(description = "Lista detalhada de tributos retidos na fonte")
        List<RetencaoTributariaRequest> retencoes,

        @Schema(description = "Observações da conferência", example = "Valores conferidos conforme medição")
        String observacao
) {
}
