package br.com.govflow.whatsapp.inbound.service;

import br.com.govflow.whatsapp.config.WhatsAppProperties;
import br.com.govflow.whatsapp.domain.entity.MensagemInboundEntity;
import br.com.govflow.whatsapp.domain.repository.MensagemInboundRepository;
import br.com.govflow.whatsapp.inbound.dto.InboundMessageDto;
import br.com.govflow.whatsapp.inbound.event.AudioRecebidoEvent;
import br.com.govflow.whatsapp.inbound.event.DocumentoRecebidoEvent;
import br.com.govflow.whatsapp.inbound.event.InboundMessageReceivedInternalEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class AsyncMediaDispatcher {

    private static final Logger log = LoggerFactory.getLogger(AsyncMediaDispatcher.class);

    private final MediaStorageHandler mediaStorageHandler;
    private final MensagemInboundRepository mensagemRepository;
    private final RabbitTemplate rabbitTemplate;
    private final WhatsAppProperties properties;

    public AsyncMediaDispatcher(
            MediaStorageHandler mediaStorageHandler,
            MensagemInboundRepository mensagemRepository,
            RabbitTemplate rabbitTemplate,
            WhatsAppProperties properties
    ) {
        this.mediaStorageHandler = mediaStorageHandler;
        this.mensagemRepository = mensagemRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    @Async("mediaTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onInboundMessageReceived(InboundMessageReceivedInternalEvent event) {
        processMediaAndDispatch(
                event.mensagemInboundId(),
                event.dto(),
                event.tenantId(),
                event.prefeituraId()
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processMediaAndDispatch(
            UUID mensagemInboundId,
            InboundMessageDto dto,
            UUID tenantId,
            UUID prefeituraId
    ) {
        log.info("Executando processamento assíncrono para mensagem ID: {}", mensagemInboundId);

        Optional<MensagemInboundEntity> entityOpt = mensagemRepository.findById(mensagemInboundId);
        if (entityOpt.isEmpty()) {
            log.error("Mensagem {} não encontrada no banco para processamento assíncrono.", mensagemInboundId);
            return;
        }

        MensagemInboundEntity entity = entityOpt.get();

        try {
            if (dto.isDocument()) {
                processDocument(entity, dto, tenantId, prefeituraId);
            } else if (dto.isAudio()) {
                processAudio(entity, dto, tenantId, prefeituraId);
            } else {
                // Mensagem de texto simples não requer upload de mídia
                entity.setProcessed(true);
                mensagemRepository.save(entity);
                log.info("Mensagem de texto {} marcada como processada.", mensagemInboundId);
            }
        } catch (Exception e) {
            log.error("Erro durante o processamento assíncrono da mensagem {}: {}", mensagemInboundId, e.getMessage(), e);
            entity.setProcessingError(e.getMessage());
            mensagemRepository.save(entity);
        }
    }

    private void processDocument(MensagemInboundEntity entity, InboundMessageDto dto, UUID tenantId, UUID prefeituraId) {
        MediaStorageHandler.StorageResult result = mediaStorageHandler.streamAndStore(
                dto.mediaUrl(),
                dto.mediaMimeType(),
                dto.fileName(),
                dto.fileSizeBytes(),
                prefeituraId,
                entity.getId()
        );

        entity.setS3Bucket(result.bucket());
        entity.setS3Key(result.key());
        entity.setMediaMimetype(result.mimeType());
        entity.setFileSizeBytes(result.sizeBytes());
        entity.setProcessed(true);
        mensagemRepository.save(entity);

        // Publica DocumentoRecebidoEvent no RabbitMQ
        DocumentoRecebidoEvent event = new DocumentoRecebidoEvent(
                tenantId,
                prefeituraId,
                entity.getId(),
                result.bucket(),
                result.key(),
                result.mimeType(),
                dto.fileName(),
                result.sizeBytes(),
                dto.senderPhone(),
                dto.senderName(),
                Instant.now()
        );

        String exchange = properties.getRabbitmq().getExchange();
        String routingKey = properties.getRabbitmq().getRoutingKeyDocumentos();

        log.info("Publicando DocumentoRecebidoEvent no RabbitMQ: exchange={}, routingKey={}, key={}",
                exchange, routingKey, result.key());

        rabbitTemplate.convertAndSend(exchange, routingKey, event, m -> {
            if (tenantId != null) {
                m.getMessageProperties().setHeader("X-Tenant-Id", tenantId.toString());
            }
            m.getMessageProperties().setHeader("X-Correlation-Id", entity.getId().toString());
            return m;
        });
    }

    private void processAudio(MensagemInboundEntity entity, InboundMessageDto dto, UUID tenantId, UUID prefeituraId) {
        MediaStorageHandler.StorageResult result = mediaStorageHandler.streamAndStore(
                dto.mediaUrl(),
                dto.mediaMimeType(),
                dto.fileName(),
                dto.fileSizeBytes(),
                prefeituraId,
                entity.getId()
        );

        entity.setS3Bucket(result.bucket());
        entity.setS3Key(result.key());
        entity.setMediaMimetype(result.mimeType());
        entity.setFileSizeBytes(result.sizeBytes());
        entity.setProcessed(true);
        mensagemRepository.save(entity);

        // Publica AudioRecebidoEvent no RabbitMQ
        AudioRecebidoEvent event = new AudioRecebidoEvent(
                tenantId,
                prefeituraId,
                entity.getId(),
                result.bucket(),
                result.key(),
                result.mimeType(),
                dto.senderPhone(),
                dto.senderName(),
                Instant.now()
        );

        String exchange = properties.getRabbitmq().getExchange();
        String routingKey = properties.getRabbitmq().getRoutingKeyAudios();

        log.info("Publicando AudioRecebidoEvent no RabbitMQ: exchange={}, routingKey={}, key={}",
                exchange, routingKey, result.key());

        rabbitTemplate.convertAndSend(exchange, routingKey, event, m -> {
            if (tenantId != null) {
                m.getMessageProperties().setHeader("X-Tenant-Id", tenantId.toString());
            }
            m.getMessageProperties().setHeader("X-Correlation-Id", entity.getId().toString());
            return m;
        });
    }
}
