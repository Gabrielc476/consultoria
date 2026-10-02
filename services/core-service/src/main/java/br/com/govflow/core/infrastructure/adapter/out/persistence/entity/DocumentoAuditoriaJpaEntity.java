package br.com.govflow.core.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tb_documentos_auditoria", schema = "core_schema")
public class DocumentoAuditoriaJpaEntity extends BaseTenantEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "documento_id", nullable = false)
    private UUID documentoId;

    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Column(name = "acao", length = 30, nullable = false)
    private String acao;

    @Column(name = "justificativa", columnDefinition = "TEXT")
    private String justificativa;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "snapshot_anterior_json", columnDefinition = "jsonb")
    private String snapshotAnteriorJson;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "snapshot_atual_json", columnDefinition = "jsonb")
    private String snapshotAtualJson;

    @Column(name = "realizado_em", nullable = false, updatable = false)
    private Instant realizadoEm;

    public DocumentoAuditoriaJpaEntity() {
    }

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

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(UUID usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getAcao() {
        return acao;
    }

    public void setAcao(String acao) {
        this.acao = acao;
    }

    public String getJustificativa() {
        return justificativa;
    }

    public void setJustificativa(String justificativa) {
        this.justificativa = justificativa;
    }

    public String getSnapshotAnteriorJson() {
        return snapshotAnteriorJson;
    }

    public void setSnapshotAnteriorJson(String snapshotAnteriorJson) {
        this.snapshotAnteriorJson = snapshotAnteriorJson;
    }

    public String getSnapshotAtualJson() {
        return snapshotAtualJson;
    }

    public void setSnapshotAtualJson(String snapshotAtualJson) {
        this.snapshotAtualJson = snapshotAtualJson;
    }

    public Instant getRealizadoEm() {
        return realizadoEm;
    }

    public void setRealizadoEm(Instant realizadoEm) {
        this.realizadoEm = realizadoEm;
    }
}
