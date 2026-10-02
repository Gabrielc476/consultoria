package br.com.govflow.core.domain.model.documento;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade de domínio pura que registra um evento imutável na trilha de auditoria do Documento.
 */
public class DocumentoAuditoria {

    private final UUID id;
    private final UUID tenantId;
    private final UUID documentoId;
    private final UUID usuarioId;
    private final String acao;
    private final String justificativa;
    private final String snapshotAnteriorJson;
    private final String snapshotAtualJson;
    private final Instant realizadoEm;

    public DocumentoAuditoria(UUID id,
                              UUID tenantId,
                              UUID documentoId,
                              UUID usuarioId,
                              String acao,
                              String justificativa,
                              String snapshotAnteriorJson,
                              String snapshotAtualJson,
                              Instant realizadoEm) {
        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = Objects.requireNonNull(tenantId, "TenantId é obrigatório para auditoria.");
        this.documentoId = Objects.requireNonNull(documentoId, "DocumentoId é obrigatório para auditoria.");
        this.usuarioId = usuarioId;
        this.acao = Objects.requireNonNull(acao, "Ação de auditoria é obrigatória.");
        this.justificativa = justificativa;
        this.snapshotAnteriorJson = snapshotAnteriorJson;
        this.snapshotAtualJson = snapshotAtualJson;
        this.realizadoEm = realizadoEm != null ? realizadoEm : Instant.now();
    }

    public static DocumentoAuditoria registrar(UUID tenantId,
                                              UUID documentoId,
                                              UUID usuarioId,
                                              String acao,
                                              String justificativa,
                                              String snapshotAnteriorJson,
                                              String snapshotAtualJson) {
        return new DocumentoAuditoria(
                UUID.randomUUID(),
                tenantId,
                documentoId,
                usuarioId,
                acao,
                justificativa,
                snapshotAnteriorJson,
                snapshotAtualJson,
                Instant.now()
        );
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getDocumentoId() {
        return documentoId;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public String getAcao() {
        return acao;
    }

    public String getJustificativa() {
        return justificativa;
    }

    public String getSnapshotAnteriorJson() {
        return snapshotAnteriorJson;
    }

    public String getSnapshotAtualJson() {
        return snapshotAtualJson;
    }

    public Instant getRealizadoEm() {
        return realizadoEm;
    }
}
