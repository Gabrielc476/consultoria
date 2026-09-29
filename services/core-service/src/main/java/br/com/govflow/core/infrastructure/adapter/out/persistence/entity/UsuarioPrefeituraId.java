package br.com.govflow.core.infrastructure.adapter.out.persistence.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class UsuarioPrefeituraId implements Serializable {

    private UUID usuarioId;
    private UUID prefeituraId;

    public UsuarioPrefeituraId() {
    }

    public UsuarioPrefeituraId(UUID usuarioId, UUID prefeituraId) {
        this.usuarioId = usuarioId;
        this.prefeituraId = prefeituraId;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UsuarioPrefeituraId that)) return false;
        return Objects.equals(usuarioId, that.usuarioId) && Objects.equals(prefeituraId, that.prefeituraId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(usuarioId, prefeituraId);
    }
}
