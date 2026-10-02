package br.com.govflow.whatsapp.inbound.event;

import br.com.govflow.whatsapp.routing.dto.ConvenioCandidatoDto;
import br.com.govflow.whatsapp.routing.dto.HistoricoMensagemDto;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public record DocumentoRecebidoEvent(
        UUID tenantId,
        UUID prefeituraId,
        UUID mensagemInboundId,
        String s3Bucket,
        String s3Key,
        String mediaMimetype,
        String fileName,
        Long fileSizeBytes,
        String senderPhone,
        String senderName,
        UUID contatoId,
        List<UUID> conveniosCandidatosIds,
        List<ConvenioCandidatoDto> conveniosCandidatos,
        List<HistoricoMensagemDto> historicoRecenteConversa,
        boolean remetenteNovo,
        Instant timestamp
) {
    public DocumentoRecebidoEvent {
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
        if (historicoRecenteConversa == null) {
            historicoRecenteConversa = Collections.emptyList();
        }
    }

    public DocumentoRecebidoEvent(
            UUID tenantId,
            UUID prefeituraId,
            UUID mensagemInboundId,
            String s3Bucket,
            String s3Key,
            String mediaMimetype,
            String fileName,
            Long fileSizeBytes,
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
                fileName,
                fileSizeBytes,
                senderPhone,
                senderName,
                null,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                false,
                timestamp
        );
    }
}
