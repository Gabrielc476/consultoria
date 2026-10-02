package br.com.govflow.whatsapp.inbound.service;

import br.com.govflow.whatsapp.config.WhatsAppProperties;
import br.com.govflow.whatsapp.domain.entity.MensagemInboundEntity;
import br.com.govflow.whatsapp.domain.repository.MensagemInboundRepository;
import br.com.govflow.whatsapp.inbound.dto.InboundMessageDto;
import br.com.govflow.whatsapp.inbound.event.AudioRecebidoEvent;
import br.com.govflow.whatsapp.inbound.event.DocumentoRecebidoEvent;
import br.com.govflow.whatsapp.inbound.event.InboundMessageReceivedInternalEvent;
import br.com.govflow.whatsapp.routing.dto.ConvenioCandidatoDto;
import br.com.govflow.whatsapp.routing.dto.HistoricoMensagemDto;
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
import java.util.Collections;
import java.util.List;
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
                event.prefeituraId(),
                event.contatoId(),
                event.conveniosCandidatos(),
                event.remetenteNovo()
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processMediaAndDispatch(
            UUID mensagemInboundId,
            InboundMessageDto dto,
            UUID tenantId,
            UUID prefeituraId
    ) {
        processMediaAndDispatch(
                mensagemInboundId,
                dto,
                tenantId,
                prefeituraId,
                null,
                Collections.emptyList(),
                false
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processMediaAndDispatch(
            UUID mensagemInboundId,
            InboundMessageDto dto,
            UUID tenantId,
            UUID prefeituraId,
            UUID contatoId,
            List<ConvenioCandidatoDto> conveniosCandidatos,
            boolean remetenteNovo
    ) {
        log.info("Executando processamento assíncrono para mensagem ID: {}", mensagemInboundId);

        Optional<MensagemInboundEntity> entityOpt = mensagemRepository.findById(mensagemInboundId);
        if (entityOpt.isEmpty()) {
            log.error("Mensagem {} não encontrada no banco para processamento assíncrono.", mensagemInboundId);
            return;
        }

        MensagemInboundEntity entity = entityOpt.get();

        try {
            if (dto.isDocument() || dto.isAudio()) {
                if (conveniosCandidatos == null || conveniosCandidatos.isEmpty() || remetenteNovo) {
                    log.info("Mídia da mensagem {} ignorada no dispatcher assíncrono: remetente não é contato vinculado a convênio. " +
                            "Apenas documentos de contatos vinculados passam pelo sistema.", mensagemInboundId);
                    entity.setProcessed(true);
                    mensagemRepository.save(entity);
                    return;
                }
            }

            if (dto.isDocument()) {
                processDocument(entity, dto, tenantId, prefeituraId, contatoId, conveniosCandidatos, remetenteNovo);
            } else if (dto.isAudio()) {
                processAudio(entity, dto, tenantId, prefeituraId, contatoId, conveniosCandidatos, remetenteNovo);
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

    private void processDocument(
            MensagemInboundEntity entity,
            InboundMessageDto dto,
            UUID tenantId,
            UUID prefeituraId,
            UUID contatoId,
            List<ConvenioCandidatoDto> conveniosCandidatos,
            boolean remetenteNovo
    ) {
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

        List<UUID> conveniosIds = conveniosCandidatos != null
                ? conveniosCandidatos.stream().map(ConvenioCandidatoDto::convenioId).toList()
                : Collections.emptyList();

        // Janela de contexto: busca últimas 5 mensagens do remetente
        List<MensagemInboundEntity> recentMessages = mensagemRepository.findTop5BySenderPhoneOrderByCreatedAtDesc(dto.senderPhone());
        List<HistoricoMensagemDto> historicoRecente = recentMessages.stream()
                .filter(m -> !m.getId().equals(entity.getId()))
                .map(m -> new HistoricoMensagemDto(
                        m.getCreatedAt(),
                        m.getMessageType(),
                        m.getContentText(),
                        m.getAudioTranscription(),
                        m.getMediaMimetype() != null ? m.getMediaMimetype() : null
                ))
                .toList();

        // Publica DocumentoRecebidoEvent enriquecido no RabbitMQ
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
                contatoId,
                conveniosIds,
                conveniosCandidatos,
                historicoRecente,
                remetenteNovo,
                Instant.now()
        );

        String exchange = properties.getRabbitmq().getExchange();
        String routingKey = properties.getRabbitmq().getRoutingKeyDocumentos();

        log.info("Publicando DocumentoRecebidoEvent enriquecido no RabbitMQ: exchange={}, routingKey={}, key={}, convenios={}, historicoMsgs={}",
                exchange, routingKey, result.key(), conveniosIds.size(), historicoRecente.size());

        rabbitTemplate.convertAndSend(exchange, routingKey, event, m -> {
            if (tenantId != null) {
                m.getMessageProperties().setHeader("X-Tenant-Id", tenantId.toString());
            }
            m.getMessageProperties().setHeader("X-Correlation-Id", entity.getId().toString());
            return m;
        });
    }

    private void processAudio(
            MensagemInboundEntity entity,
            InboundMessageDto dto,
            UUID tenantId,
            UUID prefeituraId,
            UUID contatoId,
            List<ConvenioCandidatoDto> conveniosCandidatos,
            boolean remetenteNovo
    ) {
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

        List<UUID> conveniosIds = conveniosCandidatos != null
                ? conveniosCandidatos.stream().map(ConvenioCandidatoDto::convenioId).toList()
                : Collections.emptyList();

        // Publica AudioRecebidoEvent enriquecido no RabbitMQ
        AudioRecebidoEvent event = new AudioRecebidoEvent(
                tenantId,
                prefeituraId,
                entity.getId(),
                result.bucket(),
                result.key(),
                result.mimeType(),
                dto.senderPhone(),
                dto.senderName(),
                contatoId,
                conveniosIds,
                conveniosCandidatos,
                remetenteNovo,
                Instant.now()
        );

        String exchange = properties.getRabbitmq().getExchange();
        String routingKey = properties.getRabbitmq().getRoutingKeyAudios();

        log.info("Publicando AudioRecebidoEvent enriquecido no RabbitMQ: exchange={}, routingKey={}, key={}, convenios={}",
                exchange, routingKey, result.key(), conveniosIds.size());

        rabbitTemplate.convertAndSend(exchange, routingKey, event, m -> {
            if (tenantId != null) {
                m.getMessageProperties().setHeader("X-Tenant-Id", tenantId.toString());
            }
            m.getMessageProperties().setHeader("X-Correlation-Id", entity.getId().toString());
            return m;
        });
    }
}
