package br.com.govflow.core.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "tb_documentos", schema = "core_schema")
public class DocumentoJpaEntity extends BaseTenantEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "prefeitura_id")
    private UUID prefeituraId;

    @Column(name = "convenio_id")
    private UUID convenioId;

    @Column(name = "contrato_id")
    private UUID contratoId;

    @Column(name = "medicao_id")
    private UUID medicaoId;

    @Column(name = "s3_bucket", length = 100)
    private String s3Bucket;

    @Column(name = "s3_key", length = 500)
    private String s3Key;

    @Column(name = "nome_arquivo_original", length = 255)
    private String nomeArquivoOriginal;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "tamanho_bytes")
    private Long tamanhoBytes;

    @Column(name = "status", length = 30, nullable = false)
    private String status;

    @Column(name = "tipo_documento_habil", length = 30)
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

    @Column(name = "confidence_score_geral", precision = 4, scale = 3, nullable = false)
    private BigDecimal confidenceScoreGeral;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "dados_extracao_json", columnDefinition = "jsonb")
    private String dadosExtracaoJson;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "bounding_boxes_json", columnDefinition = "jsonb")
    private String boundingBoxesJson;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "dados_revisao_json", columnDefinition = "jsonb")
    private String dadosRevisaoJson;

    @Column(name = "motivo_rejeicao", columnDefinition = "TEXT")
    private String motivoRejeicao;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPrefeituraId() {
        return prefeituraId;
    }

    public void setPrefeituraId(UUID prefeituraId) {
        this.prefeituraId = prefeituraId;
    }

    public UUID getConvenioId() {
        return convenioId;
    }

    public void setConvenioId(UUID convenioId) {
        this.convenioId = convenioId;
    }

    public UUID getContratoId() {
        return contratoId;
    }

    public void setContratoId(UUID contratoId) {
        this.contratoId = contratoId;
    }

    public UUID getMedicaoId() {
        return medicaoId;
    }

    public void setMedicaoId(UUID medicaoId) {
        this.medicaoId = medicaoId;
    }

    public String getS3Bucket() {
        return s3Bucket;
    }

    public void setS3Bucket(String s3Bucket) {
        this.s3Bucket = s3Bucket;
    }

    public String getS3Key() {
        return s3Key;
    }

    public void setS3Key(String s3Key) {
        this.s3Key = s3Key;
    }

    public String getNomeArquivoOriginal() {
        return nomeArquivoOriginal;
    }

    public void setNomeArquivoOriginal(String nomeArquivoOriginal) {
        this.nomeArquivoOriginal = nomeArquivoOriginal;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getTamanhoBytes() {
        return tamanhoBytes;
    }

    public void setTamanhoBytes(Long tamanhoBytes) {
        this.tamanhoBytes = tamanhoBytes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public BigDecimal getConfidenceScoreGeral() {
        return confidenceScoreGeral;
    }

    public void setConfidenceScoreGeral(BigDecimal confidenceScoreGeral) {
        this.confidenceScoreGeral = confidenceScoreGeral;
    }

    public String getDadosExtracaoJson() {
        return dadosExtracaoJson;
    }

    public void setDadosExtracaoJson(String dadosExtracaoJson) {
        this.dadosExtracaoJson = dadosExtracaoJson;
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

    public String getMotivoRejeicao() {
        return motivoRejeicao;
    }

    public void setMotivoRejeicao(String motivoRejeicao) {
        this.motivoRejeicao = motivoRejeicao;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
