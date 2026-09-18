package br.com.govflow.core.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Value Object contendo os dados fiscais aprovados ou editados manualmente pelo analista
 * durante a conferência Human-in-the-Loop.
 */
public record DadosRevisaoAnalista(
        DadosFiscais dadosFiscais,
        String observacao
) {
    public DadosRevisaoAnalista {
        dadosFiscais = dadosFiscais != null ? dadosFiscais : new DadosFiscais(
                null, null, null, null, null, null, null, null, null, null, null, null, Collections.emptyList()
        );
    }

    /**
     * Construtor de conveniência com todos os campos individuais para retrocompatibilidade.
     */
    public DadosRevisaoAnalista(
            TipoDocumentoHabil tipoDocumento,
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
            List<RetencaoTributaria> retencoes,
            String observacao
    ) {
        this(
                new DadosFiscais(
                        tipoDocumento,
                        numeroDocumento,
                        serieDocumento,
                        chaveAcessoNfe,
                        dataEmissao,
                        cnpjCredor,
                        razaoSocialCredor,
                        descricaoServico,
                        numeroEmpenho,
                        valorBruto,
                        valorTotalDeducoes,
                        valorLiquido,
                        retencoes
                ),
                observacao
        );
    }

    // Métodos delegados para o Value Object DadosFiscais
    public TipoDocumentoHabil tipoDocumento() {
        return dadosFiscais.tipoDocumento();
    }

    public String numeroDocumento() {
        return dadosFiscais.numeroDocumento();
    }

    public String serieDocumento() {
        return dadosFiscais.serieDocumento();
    }

    public String chaveAcessoNfe() {
        return dadosFiscais.chaveAcessoNfe();
    }

    public LocalDate dataEmissao() {
        return dadosFiscais.dataEmissao();
    }

    public String cnpjCredor() {
        return dadosFiscais.cnpjCredor();
    }

    public String razaoSocialCredor() {
        return dadosFiscais.razaoSocialCredor();
    }

    public String descricaoServico() {
        return dadosFiscais.descricaoServico();
    }

    public String numeroEmpenho() {
        return dadosFiscais.numeroEmpenho();
    }

    public BigDecimal valorBruto() {
        return dadosFiscais.valorBruto();
    }

    public BigDecimal valorTotalDeducoes() {
        return dadosFiscais.valorTotalDeducoes();
    }

    public BigDecimal valorLiquido() {
        return dadosFiscais.valorLiquido();
    }

    public List<RetencaoTributaria> retencoes() {
        return dadosFiscais.retencoes();
    }

    public Map<String, Object> gerarSnapshot() {
        Map<String, Object> snapshot = new LinkedHashMap<>(dadosFiscais.gerarSnapshot());
        snapshot.put("observacao", observacao);
        return snapshot;
    }
}
