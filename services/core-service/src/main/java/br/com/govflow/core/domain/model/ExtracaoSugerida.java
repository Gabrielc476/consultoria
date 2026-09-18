package br.com.govflow.core.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Representa os dados fiscais lidos automaticamente pela IA (OCR/Multimodal),
 * incluindo escores de confiança geral e por campo, além do resultado da validação matemática prévia.
 */
public record ExtracaoSugerida(
        DadosFiscais dadosFiscais,
        double confidenceScoreGeral,
        Map<String, Double> scoresConfiancaCampos,
        boolean consistenteMatematicamente,
        List<String> alertasInconsistencia
) {
    public ExtracaoSugerida {
        dadosFiscais = dadosFiscais != null ? dadosFiscais : new DadosFiscais(
                null, null, null, null, null, null, null, null, null, null, null, null, Collections.emptyList()
        );
        scoresConfiancaCampos = scoresConfiancaCampos != null
                ? Collections.unmodifiableMap(new LinkedHashMap<>(scoresConfiancaCampos))
                : Collections.emptyMap();
        alertasInconsistencia = alertasInconsistencia != null ? List.copyOf(alertasInconsistencia) : Collections.emptyList();
        confidenceScoreGeral = Math.max(0.0, Math.min(1.0, confidenceScoreGeral));
    }

    /**
     * Construtor de conveniência com campos individuais e mapa de confiança por campo.
     */
    public ExtracaoSugerida(
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
            double confidenceScoreGeral,
            Map<String, Double> scoresConfiancaCampos,
            boolean consistenteMatematicamente,
            List<String> alertasInconsistencia
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
                confidenceScoreGeral,
                scoresConfiancaCampos,
                consistenteMatematicamente,
                alertasInconsistencia
        );
    }

    /**
     * Construtor legado de conveniência para retrocompatibilidade.
     */
    public ExtracaoSugerida(
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
            double confidenceScoreGeral,
            boolean consistenteMatematicamente,
            List<String> alertasInconsistencia
    ) {
        this(
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
                retencoes,
                confidenceScoreGeral,
                Collections.emptyMap(),
                consistenteMatematicamente,
                alertasInconsistencia
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
        return dadosFiscais.gerarSnapshot();
    }
}
