package br.com.govflow.core.domain.event;

import java.time.Instant;
import java.util.UUID;

public record DocumentoProntoParaTransferegovEvent(
        UUID eventId,
        UUID tenantId,
        UUID documentoId,
        UUID analistaId,
        Instant occurredAt
) {
    public DocumentoProntoParaTransferegovEvent {
        if (eventId == null) {
            eventId = UUID.randomUUID();
        }
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }

    public static DocumentoProntoParaTransferegovEvent of(UUID tenantId, UUID documentoId, UUID analistaId) {
        return new DocumentoProntoParaTransferegovEvent(
                UUID.randomUUID(),
                tenantId,
                documentoId,
                analistaId,
                Instant.now()
        );
    }
}
