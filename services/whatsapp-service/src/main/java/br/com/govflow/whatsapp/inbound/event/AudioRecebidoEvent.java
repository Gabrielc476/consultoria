package br.com.govflow.whatsapp.inbound.event;

import br.com.govflow.whatsapp.routing.dto.ConvenioCandidatoDto;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
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
        UUID contatoId,
        List<UUID> conveniosCandidatosIds,
        List<ConvenioCandidatoDto> conveniosCandidatos,
        boolean remetenteNovo,
        Instant timestamp
) {
    public AudioRecebidoEvent {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
        if (conveniosCandidatos == null) {
            conveniosCandidatos = Collections.emptyList();
        }
        if (conveniosCandidatosIds == null) {
            conveniosCandidatosIds = conveniosCandidatos.stream()
                    .map(ConvenioCandidatoDto::convenioId)
                    .toList();
        }
    }

    public AudioRecebidoEvent(
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
        this(
                tenantId,
                prefeituraId,
                mensagemInboundId,
                s3Bucket,
                s3Key,
                mediaMimetype,
                senderPhone,
                senderName,
                null,
                Collections.emptyList(),
                Collections.emptyList(),
                false,
                timestamp
        );
    }
}
