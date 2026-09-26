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
@Table(name = "tb_condicionantes_suspensivas", schema = "core_schema")
public class CondicionanteSuspensivaJpaEntity extends BaseTenantEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "convenio_id", nullable = false)
    private UUID convenioId;

    @Column(name = "tipo_condicionante", length = 50, nullable = false)
    private String tipoCondicionante;

    @Column(name = "status", length = 30, nullable = false)
    private String status;

    @Column(name = "numero_documento_comprobatorio", length = 100)
    private String numeroDocumentoComprobatorio;

    @Column(name = "data_aprovacao")
    private LocalDate dataAprovacao;

    @Column(name = "data_validade")
    private LocalDate dataValidade;

    @Column(name = "observacoes_analise_caixa", columnDefinition = "TEXT")
    private String observacoesAnaliseCaixa;

    @Column(name = "s3_key_documento", length = 500)
    private String s3KeyDocumento;

    @Column(name = "data_limite_saneamento")
    private LocalDate dataLimiteSaneamento;

    @Column(name = "s3_key_laudo_pendencias", length = 500)
    private String s3KeyLaudoPendencias;

    @Column(name = "valor_orcamento_aprovado_caixa", precision = 15, scale = 2)
    private BigDecimal valorOrcamentoAprovadoCaixa;

    @Column(name = "percentual_bdi_aprovado", precision = 5, scale = 2)
    private BigDecimal percentualBdiAprovado;

    @Column(name = "numero_art_rrt", length = 50)
    private String numeroArtRrt;

    @Column(name = "orgao_emissor", length = 100)
    private String orgaoEmissor;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public CondicionanteSuspensivaJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getConvenioId() {
        return convenioId;
    }

    public void setConvenioId(UUID convenioId) {
        this.convenioId = convenioId;
    }

    public String getTipoCondicionante() {
        return tipoCondicionante;
    }

    public void setTipoCondicionante(String tipoCondicionante) {
        this.tipoCondicionante = tipoCondicionante;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNumeroDocumentoComprobatorio() {
        return numeroDocumentoComprobatorio;
    }

    public void setNumeroDocumentoComprobatorio(String numeroDocumentoComprobatorio) {
        this.numeroDocumentoComprobatorio = numeroDocumentoComprobatorio;
    }

    public LocalDate getDataAprovacao() {
        return dataAprovacao;
    }

    public void setDataAprovacao(LocalDate dataAprovacao) {
        this.dataAprovacao = dataAprovacao;
    }

    public LocalDate getDataValidade() {
        return dataValidade;
    }

    public void setDataValidade(LocalDate dataValidade) {
        this.dataValidade = dataValidade;
    }

    public String getObservacoesAnaliseCaixa() {
        return observacoesAnaliseCaixa;
    }

    public void setObservacoesAnaliseCaixa(String observacoesAnaliseCaixa) {
        this.observacoesAnaliseCaixa = observacoesAnaliseCaixa;
    }

    public String getS3KeyDocumento() {
        return s3KeyDocumento;
    }

    public void setS3KeyDocumento(String s3KeyDocumento) {
        this.s3KeyDocumento = s3KeyDocumento;
    }

    public LocalDate getDataLimiteSaneamento() {
        return dataLimiteSaneamento;
    }

    public void setDataLimiteSaneamento(LocalDate dataLimiteSaneamento) {
        this.dataLimiteSaneamento = dataLimiteSaneamento;
    }

    public String getS3KeyLaudoPendencias() {
        return s3KeyLaudoPendencias;
    }

    public void setS3KeyLaudoPendencias(String s3KeyLaudoPendencias) {
        this.s3KeyLaudoPendencias = s3KeyLaudoPendencias;
    }

    public BigDecimal getValorOrcamentoAprovadoCaixa() {
        return valorOrcamentoAprovadoCaixa;
    }

    public void setValorOrcamentoAprovadoCaixa(BigDecimal valorOrcamentoAprovadoCaixa) {
        this.valorOrcamentoAprovadoCaixa = valorOrcamentoAprovadoCaixa;
    }

    public BigDecimal getPercentualBdiAprovado() {
        return percentualBdiAprovado;
    }

    public void setPercentualBdiAprovado(BigDecimal percentualBdiAprovado) {
        this.percentualBdiAprovado = percentualBdiAprovado;
    }

    public String getNumeroArtRrt() {
        return numeroArtRrt;
    }

    public void setNumeroArtRrt(String numeroArtRrt) {
        this.numeroArtRrt = numeroArtRrt;
    }

    public String getOrgaoEmissor() {
        return orgaoEmissor;
    }

    public void setOrgaoEmissor(String orgaoEmissor) {
        this.orgaoEmissor = orgaoEmissor;
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
