package br.com.govflow.whatsapp.outbound.dto;

import java.time.Instant;

public record MessageSentResult(
        boolean success,
        String providerMessageId,
        String error,
        Instant timestamp
) {
    public static MessageSentResult ok(String providerMessageId) {
        return new MessageSentResult(true, providerMessageId, null, Instant.now());
    }

    public static MessageSentResult failed(String error) {
        return new MessageSentResult(false, null, error, Instant.now());
    }
}
