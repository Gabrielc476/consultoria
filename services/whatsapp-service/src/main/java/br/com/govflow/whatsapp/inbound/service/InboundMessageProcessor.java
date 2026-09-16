package br.com.govflow.whatsapp.inbound.service;

import br.com.govflow.whatsapp.config.WhatsAppProperties;
import br.com.govflow.whatsapp.domain.entity.MensagemInboundEntity;
import br.com.govflow.whatsapp.domain.repository.MensagemInboundRepository;
import br.com.govflow.whatsapp.inbound.dto.InboundMessageDto;
import br.com.govflow.whatsapp.inbound.dto.WebhookResponseDto;
import br.com.govflow.whatsapp.inbound.event.InboundMessageReceivedInternalEvent;
import br.com.govflow.whatsapp.inbound.parser.WebhookParserStrategy;
import br.com.govflow.whatsapp.routing.service.ContactResolutionService;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class InboundMessageProcessor {

    private static final Logger log = LoggerFactory.getLogger(InboundMessageProcessor.class);

    private final List<WebhookParserStrategy> parsers;
    private final ContactResolutionService contactResolutionService;
    private final MensagemInboundRepository mensagemRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final WhatsAppProperties properties;

    public InboundMessageProcessor(
            List<WebhookParserStrategy> parsers,
            ContactResolutionService contactResolutionService,
            MensagemInboundRepository mensagemRepository,
            ApplicationEventPublisher eventPublisher,
            WhatsAppProperties properties
    ) {
        this.parsers = parsers;
        this.contactResolutionService = contactResolutionService;
        this.mensagemRepository = mensagemRepository;
        this.eventPublisher = eventPublisher;
        this.properties = properties;
    }

    @Transactional
    public WebhookResponseDto processWebhook(JsonNode rootNode) {
        long startTime = System.currentTimeMillis();

        // 1. Encontrar parser adequado para o provedor
        String activeProvider = properties.getActiveProvider();
        WebhookParserStrategy parser = parsers.stream()
                .filter(p -> p.supports(activeProvider, rootNode))
                .findFirst()
                .orElse(null);

        if (parser == null) {
            log.warn("Nenhuma estratégia de parser encontrada para o payload recebido e provedor {}", activeProvider);
            return WebhookResponseDto.ignored("Nenhuma estratégia de parser compatível com o payload");
        }

        // 2. Efetuar parse do payload para DTO canônico
        Optional<InboundMessageDto> dtoOpt = parser.parse(rootNode);
        if (dtoOpt.isEmpty()) {
            return WebhookResponseDto.ignored("Evento ignorado (não é mensagem ou enviado pela própria instância)");
        }

        InboundMessageDto dto = dtoOpt.get();

        // 3. Checagem de Idempotência por external_message_id
        if (mensagemRepository.existsByExternalMessageId(dto.externalMessageId())) {
            log.info("Mensagem {} já processada anteriormente. Retornando 200 OK idempotente.", dto.externalMessageId());
            return WebhookResponseDto.alreadyExists(dto.externalMessageId());
        }

        // 4. Resolução de remetente (mapeamento prefeitura e tenant)
        ContactResolutionService.ResolvedContact contact = contactResolutionService.resolve(dto.senderPhone());

        // 5. Persistência inicial síncrona (< 20ms)
        MensagemInboundEntity entity = new MensagemInboundEntity();
        entity.setInstanceName(dto.instanceName());
        entity.setExternalMessageId(dto.externalMessageId());
        entity.setSenderPhone(dto.senderPhone());
        entity.setSenderName(dto.senderName());
        entity.setMessageType(dto.messageType());
        entity.setContentText(dto.contentText());
        entity.setMediaUrl(dto.mediaUrl());
        entity.setMediaMimetype(dto.mediaMimeType());
        entity.setFileSizeBytes(dto.fileSizeBytes());
        entity.setRawPayload(dto.rawPayload());
        entity.setTenantId(contact.tenantId());
        entity.setPrefeituraId(contact.prefeituraId());
        entity.setProcessed(false);

        MensagemInboundEntity savedEntity = mensagemRepository.save(entity);

        // 6. Publica evento interno que será consumido após o commit da transação
        eventPublisher.publishEvent(new InboundMessageReceivedInternalEvent(
                savedEntity.getId(),
                dto,
                contact.tenantId(),
                contact.prefeituraId()
        ));

        long duration = System.currentTimeMillis() - startTime;
        log.info("Webhook síncrono concluído em {}ms para mensagem ID: {}", duration, dto.externalMessageId());

        return WebhookResponseDto.received(dto.externalMessageId());
    }
}
