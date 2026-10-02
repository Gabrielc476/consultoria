package br.com.govflow.whatsapp.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tb_contato_convenios", schema = "whatsapp_schema")
public class ContatoConvenioEntity {

    @EmbeddedId
    private ContatoConvenioId id;

    @Column(name = "prefeitura_id", nullable = false)
    private UUID prefeituraId;

    @Column(name = "papel_especifico", length = 100)
    private String papelEspecifico;

    @Column(name = "principal", nullable = false)
    private boolean principal = false;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm = Instant.now();

    public ContatoConvenioEntity() {
    }

    public ContatoConvenioEntity(UUID contatoId, UUID convenioId, UUID prefeituraId, String papelEspecifico, boolean principal) {
        this.id = new ContatoConvenioId(contatoId, convenioId);
        this.prefeituraId = prefeituraId;
        this.papelEspecifico = papelEspecifico;
        this.principal = principal;
        this.criadoEm = Instant.now();
    }

    public ContatoConvenioId getId() {
        return id;
    }

    public void setId(ContatoConvenioId id) {
        this.id = id;
    }

    public UUID getContatoId() {
        return id != null ? id.getContatoId() : null;
    }

    public UUID getConvenioId() {
        return id != null ? id.getConvenioId() : null;
    }

    public UUID getPrefeituraId() {
        return prefeituraId;
    }

    public void setPrefeituraId(UUID prefeituraId) {
        this.prefeituraId = prefeituraId;
    }

    public String getPapelEspecifico() {
        return papelEspecifico;
    }

    public void setPapelEspecifico(String papelEspecifico) {
        this.papelEspecifico = papelEspecifico;
    }

    public boolean isPrincipal() {
        return principal;
    }

    public void setPrincipal(boolean principal) {
        this.principal = principal;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(Instant criadoEm) {
        this.criadoEm = criadoEm;
    }
}
