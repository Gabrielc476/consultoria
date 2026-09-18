package br.com.govflow.core.infrastructure.adapter.in.amqp;

import br.com.govflow.core.application.port.in.ProcessarDocumentoExtraidoUseCase;
import br.com.govflow.core.domain.model.BoundingBox;
import br.com.govflow.core.domain.model.ExtracaoSugerida;
import br.com.govflow.core.infrastructure.adapter.in.amqp.dto.DocumentoExtraidoEventDto;
import br.com.govflow.core.infrastructure.adapter.in.amqp.dto.DocumentoExtraidoPayloadDto;
import br.com.govflow.core.infrastructure.interceptor.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class DocumentoProcessadoListener {

    private static final Logger log = LoggerFactory.getLogger(DocumentoProcessadoListener.class);

    private final ProcessarDocumentoExtraidoUseCase processarUseCase;
    private final DocumentoExtraidoPayloadParser parser;

    public DocumentoProcessadoListener(ProcessarDocumentoExtraidoUseCase processarUseCase,
                                      DocumentoExtraidoPayloadParser parser) {
        this.processarUseCase = processarUseCase;
        this.parser = parser;
    }

    @RabbitListener(queues = "${govflow.rabbitmq.queue-documentos-processados:fila.documentos.processados}")
    public void onDocumentoExtraido(DocumentoExtraidoEventDto event) {
        log.info("Recebido DocumentoExtraidoEvent: eventId={}, correlationId={}, tenantId={}",
                event.eventId(), event.correlationId(), event.tenantId());

        if (event == null || event.payload() == null) {
            log.error("DocumentoExtraidoEvent recebido nulo ou com payload vazio. Encaminhando para DLQ.");
            throw new org.springframework.amqp.AmqpRejectAndDontRequeueException("Payload ausente no evento DocumentoExtraidoEvent");
        }

        UUID tenantId = event.tenantId();
        if (tenantId == null) {
            log.error("DocumentoExtraidoEvent recebido sem tenantId! Encaminhando para DLQ.");
            throw new org.springframework.amqp.AmqpRejectAndDontRequeueException("tenantId obrigatório não informado no evento DocumentoExtraidoEvent");
        }

        TenantContext.setCurrentTenant(tenantId);
        try {
            DocumentoExtraidoPayloadDto payload = event.payload();
            ExtracaoSugerida extracao = parser.parseExtracao(payload);
            Map<String, BoundingBox> boxes = parser.parseBoundingBoxes(payload);

            UUID docId = payload.documentoId() != null ? payload.documentoId() : event.correlationId();

            ProcessarDocumentoExtraidoUseCase.ProcessarDocumentoExtraidoCommand command =
                    new ProcessarDocumentoExtraidoUseCase.ProcessarDocumentoExtraidoCommand(
                            tenantId,
                            docId,
                            payload.prefeituraId(),
                            payload.convenioId(),
                            payload.s3Bucket(),
                            payload.s3Key(),
                            payload.nomeArquivoOriginal(),
                            payload.contentType(),
                            payload.tamanhoBytes(),
                            extracao,
                            boxes
                    );

            processarUseCase.processar(command);
            log.info("Documento {} processado com sucesso para o estado EM_CONFERENCIA", docId);
        } catch (Exception e) {
            log.error("Falha ao processar DocumentoExtraidoEvent: eventId={}", event.eventId(), e);
            throw e;
        } finally {
            TenantContext.clear();
        }
    }
}
