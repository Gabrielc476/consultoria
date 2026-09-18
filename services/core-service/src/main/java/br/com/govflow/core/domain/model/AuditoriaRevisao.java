package br.com.govflow.core.domain.model;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class AuditoriaRevisao {

    private final UUID id;
    private final UUID tenantId;
    private final UUID documentoId;
    private final UUID analistaId;
    private final AcaoAuditoria acao;
    private final Instant dataRevisao;
    private final String justificativa;
    private final DiffRevisao diff;
    private final Map<String, Object> valoresOriginais;
    private final Map<String, Object> valoresRevisados;

    public AuditoriaRevisao(UUID id,
                            UUID tenantId,
                            UUID documentoId,
                            UUID analistaId,
                            AcaoAuditoria acao,
                            Instant dataRevisao,
                            String justificativa,
                            DiffRevisao diff,
                            Map<String, Object> valoresOriginais,
                            Map<String, Object> valoresRevisados) {
        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = Objects.requireNonNull(tenantId, "TenantId é obrigatório para AuditoriaRevisao.");
        this.documentoId = Objects.requireNonNull(documentoId, "DocumentoId é obrigatório para AuditoriaRevisao.");
        this.analistaId = Objects.requireNonNull(analistaId, "AnalistaId é obrigatório para AuditoriaRevisao.");
        this.acao = Objects.requireNonNull(acao, "Ação de auditoria é obrigatória.");
        this.dataRevisao = dataRevisao != null ? dataRevisao : Instant.now();
        this.justificativa = justificativa;
        this.diff = diff != null ? diff : new DiffRevisao(Collections.emptyMap());
        this.valoresOriginais = valoresOriginais != null ? Collections.unmodifiableMap(new LinkedHashMap<>(valoresOriginais)) : Collections.emptyMap();
        this.valoresRevisados = valoresRevisados != null ? Collections.unmodifiableMap(new LinkedHashMap<>(valoresRevisados)) : Collections.emptyMap();
    }

    public static AuditoriaRevisao criarAprovacao(UUID tenantId,
                                                  UUID documentoId,
                                                  UUID analistaId,
                                                  String observacao,
                                                  DiffRevisao diff,
                                                  Map<String, Object> valoresOriginais,
                                                  Map<String, Object> valoresRevisados) {
        return new AuditoriaRevisao(
                UUID.randomUUID(),
                tenantId,
                documentoId,
                analistaId,
                AcaoAuditoria.APROVACAO,
                Instant.now(),
                observacao,
                diff,
                valoresOriginais,
                valoresRevisados
        );
    }

    public static AuditoriaRevisao criarRejeicao(UUID tenantId,
                                                 UUID documentoId,
                                                 UUID analistaId,
                                                 String motivo,
                                                 Map<String, Object> valoresOriginais) {
        return new AuditoriaRevisao(
                UUID.randomUUID(),
                tenantId,
                documentoId,
                analistaId,
                AcaoAuditoria.REJEICAO,
                Instant.now(),
                motivo,
                new DiffRevisao(Collections.emptyMap()),
                valoresOriginais,
                Collections.emptyMap()
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

    public UUID getAnalistaId() {
        return analistaId;
    }

    public AcaoAuditoria getAcao() {
        return acao;
    }

    public Instant getDataRevisao() {
        return dataRevisao;
    }

    public String getJustificativa() {
        return justificativa;
    }

    public DiffRevisao getDiff() {
        return diff;
    }

    public Map<String, Object> getValoresOriginais() {
        return valoresOriginais;
    }

    public Map<String, Object> getValoresRevisados() {
        return valoresRevisados;
    }
}
