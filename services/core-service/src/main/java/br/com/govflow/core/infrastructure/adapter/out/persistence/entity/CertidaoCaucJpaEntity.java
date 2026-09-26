package br.com.govflow.core.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "tb_certidoes_cauc", schema = "core_schema")
public class CertidaoCaucJpaEntity extends BaseTenantEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "prefeitura_id", nullable = false)
    private UUID prefeituraId;

    @Column(name = "tipo_exigencia", length = 50, nullable = false)
    private String tipoExigencia;

    @Column(name = "numero_certidao", length = 100)
    private String numeroCertidao;

    @Column(name = "data_emissao", nullable = false)
    private LocalDate dataEmissao;

    @Column(name = "data_validade", nullable = false)
    private LocalDate dataValidade;

    @Column(name = "situacao", length = 30, nullable = false)
    private String situacao;

    @Column(name = "dias_para_vencer")
    private Integer diasParaVencer;

    @Column(name = "s3_key_comprovante", length = 500)
    private String s3KeyComprovante;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public CertidaoCaucJpaEntity() {
    }

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

    public String getTipoExigencia() {
        return tipoExigencia;
    }

    public void setTipoExigencia(String tipoExigencia) {
        this.tipoExigencia = tipoExigencia;
    }

    public String getNumeroCertidao() {
        return numeroCertidao;
    }

    public void setNumeroCertidao(String numeroCertidao) {
        this.numeroCertidao = numeroCertidao;
    }

    public LocalDate getDataEmissao() {
        return dataEmissao;
    }

    public void setDataEmissao(LocalDate dataEmissao) {
        this.dataEmissao = dataEmissao;
    }

    public LocalDate getDataValidade() {
        return dataValidade;
    }

    public void setDataValidade(LocalDate dataValidade) {
        this.dataValidade = dataValidade;
    }

    public String getSituacao() {
        return situacao;
    }

    public void setSituacao(String situacao) {
        this.situacao = situacao;
    }

    public Integer getDiasParaVencer() {
        return diasParaVencer;
    }

    public void setDiasParaVencer(Integer diasParaVencer) {
        this.diasParaVencer = diasParaVencer;
    }

    public String getS3KeyComprovante() {
        return s3KeyComprovante;
    }

    public void setS3KeyComprovante(String s3KeyComprovante) {
        this.s3KeyComprovante = s3KeyComprovante;
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
