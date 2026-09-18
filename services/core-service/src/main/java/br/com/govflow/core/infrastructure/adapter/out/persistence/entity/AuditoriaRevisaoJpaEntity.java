package br.com.govflow.core.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tb_auditorias_revisao", schema = "core_schema")
public class AuditoriaRevisaoJpaEntity extends BaseTenantEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "documento_id", nullable = false, updatable = false)
    private UUID documentoId;

    @Column(name = "analista_id", nullable = false, updatable = false)
    private UUID analistaId;

    @Column(name = "acao", length = 30, nullable = false, updatable = false)
    private String acao;

    @Column(name = "data_revisao", nullable = false, updatable = false)
    private Instant dataRevisao;

    @Column(name = "justificativa", columnDefinition = "TEXT", updatable = false)
    private String justificativa;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "diff_alteracoes_json", columnDefinition = "jsonb", updatable = false)
    private String diffAlteracoesJson;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "valores_originais_json", columnDefinition = "jsonb", updatable = false)
    private String valoresOriginaisJson;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "valores_revisados_json", columnDefinition = "jsonb", updatable = false)
    private String valoresRevisadosJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getDocumentoId() {
        return documentoId;
    }

    public void setDocumentoId(UUID documentoId) {
        this.documentoId = documentoId;
    }

    public UUID getAnalistaId() {
        return analistaId;
    }

    public void setAnalistaId(UUID analistaId) {
        this.analistaId = analistaId;
    }

    public String getAcao() {
        return acao;
    }

    public void setAcao(String acao) {
        this.acao = acao;
    }

    public Instant getDataRevisao() {
        return dataRevisao;
    }

    public void setDataRevisao(Instant dataRevisao) {
        this.dataRevisao = dataRevisao;
    }

    public String getJustificativa() {
        return justificativa;
    }

    public void setJustificativa(String justificativa) {
        this.justificativa = justificativa;
    }

    public String getDiffAlteracoesJson() {
        return diffAlteracoesJson;
    }

    public void setDiffAlteracoesJson(String diffAlteracoesJson) {
        this.diffAlteracoesJson = diffAlteracoesJson;
    }

    public String getValoresOriginaisJson() {
        return valoresOriginaisJson;
    }

    public void setValoresOriginaisJson(String valoresOriginaisJson) {
        this.valoresOriginaisJson = valoresOriginaisJson;
    }

    public String getValoresRevisadosJson() {
        return valoresRevisadosJson;
    }

    public void setValoresRevisadosJson(String valoresRevisadosJson) {
        this.valoresRevisadosJson = valoresRevisadosJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
