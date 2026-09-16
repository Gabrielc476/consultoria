package br.com.govflow.whatsapp.inbound.event;

import java.time.Instant;
import java.util.UUID;

public record AudioRecebidoEvent(
        UUID tenantId,
        UUID prefeituraId,
        UUID mensagemInboundId,
        String s3Bucket,
        String s3Key,
        String mediaMimetype,
        String senderPhone,
        String senderName,
        Instant timestamp
) {
    public AudioRecebidoEvent {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }
}
