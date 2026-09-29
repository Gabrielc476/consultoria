package br.com.govflow.core.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tb_usuario_prefeituras", schema = "core_schema")
@IdClass(UsuarioPrefeituraId.class)
public class UsuarioPrefeituraJpaEntity {

    @Id
    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Id
    @Column(name = "prefeitura_id", nullable = false)
    private UUID prefeituraId;

    @Column(name = "atribuido_em", nullable = false)
    private Instant atribuidoEm;

    public UsuarioPrefeituraJpaEntity() {
    }

    public UsuarioPrefeituraJpaEntity(UUID usuarioId, UUID prefeituraId) {
        this.usuarioId = usuarioId;
        this.prefeituraId = prefeituraId;
        this.atribuidoEm = Instant.now();
    }

    public UsuarioPrefeituraJpaEntity(UUID usuarioId, UUID prefeituraId, Instant atribuidoEm) {
        this.usuarioId = usuarioId;
        this.prefeituraId = prefeituraId;
        this.atribuidoEm = atribuidoEm != null ? atribuidoEm : Instant.now();
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(UUID usuarioId) {
        this.usuarioId = usuarioId;
    }

    public UUID getPrefeituraId() {
        return prefeituraId;
    }

    public void setPrefeituraId(UUID prefeituraId) {
        this.prefeituraId = prefeituraId;
    }

    public Instant getAtribuidoEm() {
        return atribuidoEm;
    }

    public void setAtribuidoEm(Instant atribuidoEm) {
        this.atribuidoEm = atribuidoEm;
    }
}
