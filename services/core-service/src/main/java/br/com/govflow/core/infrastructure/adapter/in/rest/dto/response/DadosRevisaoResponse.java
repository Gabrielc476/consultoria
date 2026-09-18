package br.com.govflow.core.infrastructure.adapter.in.rest.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Dados finais revisados e aprovados pelo analista")
public record DadosRevisaoResponse(
        String tipoDocumento,
        String numeroDocumento,
        String serieDocumento,
        String chaveAcessoNfe,
        LocalDate dataEmissao,
        String cnpjCredor,
        String razaoSocialCredor,
        String descricaoServico,
        String numeroEmpenho,
        BigDecimal valorBruto,
        BigDecimal valorTotalDeducoes,
        BigDecimal valorLiquido,
        List<RetencaoResponse> retencoes,
        String observacao
) {
}
