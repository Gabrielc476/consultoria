package br.com.govflow.whatsapp.inbound.event;

import br.com.govflow.whatsapp.inbound.dto.InboundMessageDto;
import br.com.govflow.whatsapp.routing.dto.ConvenioCandidatoDto;

import java.util.List;
import java.util.UUID;

public record InboundMessageReceivedInternalEvent(
        UUID mensagemInboundId,
        InboundMessageDto dto,
        UUID tenantId,
        UUID prefeituraId,
        UUID contatoId,
        List<ConvenioCandidatoDto> conveniosCandidatos,
        boolean remetenteNovo
) {}
