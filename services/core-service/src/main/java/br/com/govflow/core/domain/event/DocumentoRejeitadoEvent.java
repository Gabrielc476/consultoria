package br.com.govflow.core.domain.event;

import java.time.Instant;
import java.util.UUID;

public record DocumentoRejeitadoEvent(
        UUID eventId,
        UUID tenantId,
        UUID documentoId,
        UUID analistaId,
        String motivo,
        Instant occurredAt
) {
    public DocumentoRejeitadoEvent {
        if (eventId == null) {
            eventId = UUID.randomUUID();
        }
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }

    public static DocumentoRejeitadoEvent of(UUID tenantId, UUID documentoId, UUID analistaId, String motivo) {
        return new DocumentoRejeitadoEvent(
                UUID.randomUUID(),
                tenantId,
                documentoId,
                analistaId,
                motivo,
                Instant.now()
        );
    }
}
