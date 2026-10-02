package br.com.govflow.core.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "tb_documentos_habeis_dados", schema = "core_schema")
public class DocumentoHabilDadosJpaEntity {

    @Id
    @Column(name = "documento_id", nullable = false)
    private UUID documentoId;

    @Column(name = "tipo_documento_habil", length = 30, nullable = false)
    private String tipoDocumentoHabil;

    @Column(name = "numero_documento", length = 50)
    private String numeroDocumento;

    @Column(name = "serie_documento", length = 10)
    private String serieDocumento;

    @Column(name = "chave_acesso_nfe", length = 44)
    private String chaveAcessoNfe;

    @Column(name = "data_emissao")
    private LocalDate dataEmissao;

    @Column(name = "cnpj_credor", length = 18)
    private String cnpjCredor;

    @Column(name = "razao_social_credor", length = 200)
    private String razaoSocialCredor;

    @Column(name = "descricao_servico", columnDefinition = "TEXT")
    private String descricaoServico;

    @Column(name = "valor_bruto", precision = 15, scale = 2)
    private BigDecimal valorBruto;

    @Column(name = "valor_total_deducoes", precision = 15, scale = 2)
    private BigDecimal valorTotalDeducoes;

    @Column(name = "valor_liquido", precision = 15, scale = 2)
    private BigDecimal valorLiquido;

    @Column(name = "status_validacao_matematica", nullable = false)
    private boolean statusValidacaoMatematica;

    @Column(name = "confidence_score_ia", precision = 4, scale = 3)
    private BigDecimal confidenceScoreIa;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "dados_extracao_ia_json", columnDefinition = "jsonb")
    private String dadosExtracaoIaJson;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "bounding_boxes_json", columnDefinition = "jsonb")
    private String boundingBoxesJson;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "dados_revisao_json", columnDefinition = "jsonb")
    private String dadosRevisaoJson;

    public DocumentoHabilDadosJpaEntity() {
    }

    public UUID getDocumentoId() {
        return documentoId;
    }

    public void setDocumentoId(UUID documentoId) {
        this.documentoId = documentoId;
    }

    public String getTipoDocumentoHabil() {
        return tipoDocumentoHabil;
    }

    public void setTipoDocumentoHabil(String tipoDocumentoHabil) {
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
