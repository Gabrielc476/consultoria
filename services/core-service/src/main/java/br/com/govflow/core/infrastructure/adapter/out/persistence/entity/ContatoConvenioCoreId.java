package br.com.govflow.core.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class ContatoConvenioCoreId implements Serializable {

    @Column(name = "contato_id", nullable = false)
    private UUID contatoId;

    @Column(name = "convenio_id", nullable = false)
    private UUID convenioId;

    public ContatoConvenioCoreId() {
    }

    public ContatoConvenioCoreId(UUID contatoId, UUID convenioId) {
        this.contatoId = contatoId;
        this.convenioId = convenioId;
    }

    public UUID getContatoId() {
        return contatoId;
    }

    public void setContatoId(UUID contatoId) {
        this.contatoId = contatoId;
    }

    public UUID getConvenioId() {
        return convenioId;
    }

    public void setConvenioId(UUID convenioId) {
        this.convenioId = convenioId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContatoConvenioCoreId that)) return false;
        return Objects.equals(contatoId, that.contatoId) && Objects.equals(convenioId, that.convenioId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contatoId, convenioId);
    }
}
