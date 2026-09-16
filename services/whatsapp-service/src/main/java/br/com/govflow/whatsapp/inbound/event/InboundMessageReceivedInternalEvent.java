package br.com.govflow.whatsapp.inbound.event;

import br.com.govflow.whatsapp.inbound.dto.InboundMessageDto;

import java.util.UUID;

public record InboundMessageReceivedInternalEvent(
        UUID mensagemInboundId,
        InboundMessageDto dto,
        UUID tenantId,
        UUID prefeituraId
) {}
