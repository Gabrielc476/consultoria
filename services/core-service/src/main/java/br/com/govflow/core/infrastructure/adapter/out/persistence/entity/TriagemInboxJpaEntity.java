package br.com.govflow.core.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tb_triagem_inbox", schema = "core_schema")
public class TriagemInboxJpaEntity extends BaseTenantEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "agente_responsavel_id")
    private UUID agenteResponsavelId;

    @Column(name = "mensagem_inbound_id")
    private UUID mensagemInboundId;

    @Column(name = "documento_id")
    private UUID documentoId;

    @Column(name = "convenio_sugerido_id")
    private UUID convenioSugeridoId;

    @Column(name = "fase_sugerida", length = 40)
    private String faseSugerida;

    @Column(name = "confidence_score", precision = 4, scale = 3, nullable = false)
    private BigDecimal confidenceScore = BigDecimal.ZERO;

    @Column(name = "motivo_ambiguidade", columnDefinition = "TEXT")
    private String motivoAmbiguidade;

    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    @Column(name = "sender_name", length = 150)
    private String senderName;

    @Column(name = "push_name", length = 150)
    private String pushName;

    @Column(name = "remetente_novo", nullable = false)
    private boolean remetenteNovo = false;

    @Column(name = "conteudo_resumo", columnDefinition = "TEXT")
    private String conteudoResumo;

    @Column(name = "status", length = 30, nullable = false)
    private String status = "PENDENTE";

    @Column(name = "resolvido_em")
    private Instant resolvidoEm;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public TriagemInboxJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getAgenteResponsavelId() {
        return agenteResponsavelId;
    }

    public void setAgenteResponsavelId(UUID agenteResponsavelId) {
        this.agenteResponsavelId = agenteResponsavelId;
    }

    public UUID getMensagemInboundId() {
        return mensagemInboundId;
    }

    public void setMensagemInboundId(UUID mensagemInboundId) {
        this.mensagemInboundId = mensagemInboundId;
    }

    public UUID getDocumentoId() {
        return documentoId;
    }

    public void setDocumentoId(UUID documentoId) {
        this.documentoId = documentoId;
    }

    public UUID getConvenioSugeridoId() {
        return convenioSugeridoId;
    }

    public void setConvenioSugeridoId(UUID convenioSugeridoId) {
        this.convenioSugeridoId = convenioSugeridoId;
    }

    public String getFaseSugerida() {
        return faseSugerida;
    }

    public void setFaseSugerida(String faseSugerida) {
        this.faseSugerida = faseSugerida;
    }

    public BigDecimal getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(BigDecimal confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public String getMotivoAmbiguidade() {
        return motivoAmbiguidade;
    }

    public void setMotivoAmbiguidade(String motivoAmbiguidade) {
        this.motivoAmbiguidade = motivoAmbiguidade;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getPushName() {
        return pushName;
    }

    public void setPushName(String pushName) {
        this.pushName = pushName;
    }

    public boolean isRemetenteNovo() {
        return remetenteNovo;
    }

    public void setRemetenteNovo(boolean remetenteNovo) {
        this.remetenteNovo = remetenteNovo;
    }

    public String getConteudoResumo() {
        return conteudoResumo;
    }

    public void setConteudoResumo(String conteudoResumo) {
        this.conteudoResumo = conteudoResumo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getResolvidoEm() {
        return resolvidoEm;
    }

    public void setResolvidoEm(Instant resolvidoEm) {
        this.resolvidoEm = resolvidoEm;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
