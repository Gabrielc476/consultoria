package br.com.govflow.core.domain.model.documento;

import br.com.govflow.core.domain.model.TipoDocumentoHabil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Modelo de domínio puro para os dados especializados e fiscais de Documentos Hábeis
 * (Notas Fiscais de Serviços, Mercadorias e Recibos Legais), mapeado para a tabela satélite
 * tb_documentos_habeis_dados.
 */
public class DocumentoHabilDados {

    private final UUID documentoId;
    private TipoDocumentoHabil tipoDocumentoHabil;
    private String numeroDocumento;
    private String serieDocumento;
    private String chaveAcessoNfe;
    private LocalDate dataEmissao;
    private String cnpjCredor;
    private String razaoSocialCredor;
    private String descricaoServico;
    private BigDecimal valorBruto;
    private BigDecimal valorTotalDeducoes;
    private BigDecimal valorLiquido;
    private boolean statusValidacaoMatematica;
    private BigDecimal confidenceScoreIa;
    private String dadosExtracaoIaJson;
    private String boundingBoxesJson;
    private String dadosRevisaoJson;

    public DocumentoHabilDados(UUID documentoId,
                               TipoDocumentoHabil tipoDocumentoHabil,
                               String numeroDocumento,
                               String serieDocumento,
                               String chaveAcessoNfe,
                               LocalDate dataEmissao,
                               String cnpjCredor,
                               String razaoSocialCredor,
                               String descricaoServico,
                               BigDecimal valorBruto,
                               BigDecimal valorTotalDeducoes,
                               BigDecimal valorLiquido,
                               boolean statusValidacaoMatematica,
                               BigDecimal confidenceScoreIa,
                               String dadosExtracaoIaJson,
                               String boundingBoxesJson,
                               String dadosRevisaoJson) {
        this.documentoId = Objects.requireNonNull(documentoId, "documentoId é obrigatório para DocumentoHabilDados.");
        this.tipoDocumentoHabil = tipoDocumentoHabil != null ? tipoDocumentoHabil : TipoDocumentoHabil.NOTA_FISCAL_SERVICOS;
        this.numeroDocumento = numeroDocumento;
        this.serieDocumento = serieDocumento;
        this.chaveAcessoNfe = chaveAcessoNfe;
        this.dataEmissao = dataEmissao;
        this.cnpjCredor = cnpjCredor;
        this.razaoSocialCredor = razaoSocialCredor;
        this.descricaoServico = descricaoServico;
        this.valorBruto = valorBruto;
        this.valorTotalDeducoes = valorTotalDeducoes != null ? valorTotalDeducoes : BigDecimal.ZERO;
        this.valorLiquido = valorLiquido;
        this.statusValidacaoMatematica = statusValidacaoMatematica;
        this.confidenceScoreIa = confidenceScoreIa != null ? confidenceScoreIa : BigDecimal.ZERO;
        this.dadosExtracaoIaJson = dadosExtracaoIaJson;
        this.boundingBoxesJson = boundingBoxesJson;
        this.dadosRevisaoJson = dadosRevisaoJson;
    }

    public UUID getDocumentoId() {
        return documentoId;
    }

    public TipoDocumentoHabil getTipoDocumentoHabil() {
        return tipoDocumentoHabil;
    }

    public void setTipoDocumentoHabil(TipoDocumentoHabil tipoDocumentoHabil) {
        this.tipoDocumentoHabil = tipoDocumentoHabil;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public void setNumeroDocumento(String numeroDocumento) {
        this.numeroDocumento = numeroDocumento;
    }

    public String getSerieDocumento() {
        return serieDocumento;
    }

    public void setSerieDocumento(String serieDocumento) {
        this.serieDocumento = serieDocumento;
    }

    public String getChaveAcessoNfe() {
        return chaveAcessoNfe;
    }

    public void setChaveAcessoNfe(String chaveAcessoNfe) {
        this.chaveAcessoNfe = chaveAcessoNfe;
    }

    public LocalDate getDataEmissao() {
        return dataEmissao;
    }

    public void setDataEmissao(LocalDate dataEmissao) {
        this.dataEmissao = dataEmissao;
    }

    public String getCnpjCredor() {
        return cnpjCredor;
    }

    public void setCnpjCredor(String cnpjCredor) {
        this.cnpjCredor = cnpjCredor;
    }

    public String getRazaoSocialCredor() {
        return razaoSocialCredor;
    }

    public void setRazaoSocialCredor(String razaoSocialCredor) {
        this.razaoSocialCredor = razaoSocialCredor;
    }

    public String getDescricaoServico() {
        return descricaoServico;
    }

    public void setDescricaoServico(String descricaoServico) {
        this.descricaoServico = descricaoServico;
    }

    public BigDecimal getValorBruto() {
        return valorBruto;
    }

    public void setValorBruto(BigDecimal valorBruto) {
        this.valorBruto = valorBruto;
    }

    public BigDecimal getValorTotalDeducoes() {
        return valorTotalDeducoes;
    }

    public void setValorTotalDeducoes(BigDecimal valorTotalDeducoes) {
        this.valorTotalDeducoes = valorTotalDeducoes;
    }

    public BigDecimal getValorLiquido() {
        return valorLiquido;
    }

    public void setValorLiquido(BigDecimal valorLiquido) {
        this.valorLiquido = valorLiquido;
    }

    public boolean isStatusValidacaoMatematica() {
        return statusValidacaoMatematica;
    }

    public void setStatusValidacaoMatematica(boolean statusValidacaoMatematica) {
        this.statusValidacaoMatematica = statusValidacaoMatematica;
    }

    public BigDecimal getConfidenceScoreIa() {
        return confidenceScoreIa;
    }

    public void setConfidenceScoreIa(BigDecimal confidenceScoreIa) {
        this.confidenceScoreIa = confidenceScoreIa;
    }

    public String getDadosExtracaoIaJson() {
        return dadosExtracaoIaJson;
    }

    public void setDadosExtracaoIaJson(String dadosExtracaoIaJson) {
        this.dadosExtracaoIaJson = dadosExtracaoIaJson;
    }

    public String getBoundingBoxesJson() {
        return boundingBoxesJson;
    }

    public void setBoundingBoxesJson(String boundingBoxesJson) {
        this.boundingBoxesJson = boundingBoxesJson;
    }

    public String getDadosRevisaoJson() {
        return dadosRevisaoJson;
    }

    public void setDadosRevisaoJson(String dadosRevisaoJson) {
        this.dadosRevisaoJson = dadosRevisaoJson;
    }
}
