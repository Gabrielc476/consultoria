package br.com.govflow.core.infrastructure.adapter.in.rest.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Schema(description = "Campos e valores fiscais extraídos pelo motor de inteligência artificial")
public record ExtracaoSugeridaResponse(
        @Schema(description = "Tipo sugerido do documento", example = "NOTA_FISCAL_SERVICOS")
        String tipoDocumento,
        @Schema(description = "Número da nota fiscal", example = "000123")
        String numeroDocumento,
        @Schema(description = "Série do documento", example = "1")
        String serieDocumento,
        @Schema(description = "Chave de acesso NFe (44 dígitos)")
        String chaveAcessoNfe,
        @Schema(description = "Data de emissão", example = "2026-05-10")
        LocalDate dataEmissao,
        @Schema(description = "CNPJ do prestador", example = "12.345.678/0001-90")
        String cnpjCredor,
        @Schema(description = "Razão social do prestador", example = "Construtora Progresso Ltda")
        String razaoSocialCredor,
        @Schema(description = "Descrição dos serviços")
        String descricaoServico,
        @Schema(description = "Número de empenho")
        String numeroEmpenho,
        @Schema(description = "Valor bruto total", example = "10000.00")
        BigDecimal valorBruto,
        @Schema(description = "Valor total das deduções", example = "1100.00")
        BigDecimal valorTotalDeducoes,
        @Schema(description = "Valor líquido", example = "8900.00")
        BigDecimal valorLiquido,
        @Schema(description = "Lista de tributos retidos")
        List<RetencaoResponse> retencoes,
        @Schema(description = "Score geral de confiança da IA (0.00 a 1.00)", example = "0.985")
        double confidenceScoreGeral,
        @Schema(description = "Scores de confiança da IA por campo individual (para semáforo da UI)")
        Map<String, Double> scoresConfiancaCampos,
        @Schema(description = "Indica se a validação matemática foi consistente", example = "true")
        boolean consistenteMatematicamente,
        @Schema(description = "Alertas de divergência matemática ou inconsistências")
        List<String> alertasInconsistencia
) {
}
