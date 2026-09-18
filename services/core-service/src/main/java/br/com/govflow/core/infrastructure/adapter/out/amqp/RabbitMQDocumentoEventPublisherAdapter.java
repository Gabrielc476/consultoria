package br.com.govflow.core.infrastructure.adapter.out.amqp;

import br.com.govflow.core.application.port.out.DocumentoEventPublisherPort;
import br.com.govflow.core.domain.event.DocumentoProntoParaTransferegovEvent;
import br.com.govflow.core.domain.event.DocumentoRejeitadoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RabbitMQDocumentoEventPublisherAdapter implements DocumentoEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQDocumentoEventPublisherAdapter.class);

    private final RabbitTemplate rabbitTemplate;

    @Value("${govflow.rabbitmq.exchange:govflow.events}")
    private String exchange;

    @Value("${govflow.rabbitmq.routing-key-documento-pronto:documento.pronto.transferegov}")
    private String routingKeyDocumentoPronto;

    @Value("${govflow.rabbitmq.routing-key-documento-rejeitado:documento.rejeitado}")
    private String routingKeyDocumentoRejeitado;

    public RabbitMQDocumentoEventPublisherAdapter(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publicarDocumentoPronto(DocumentoProntoParaTransferegovEvent evento) {
        executarAposCommit(() -> executarPublicacaoDocumentoPronto(evento), "DocumentoProntoParaTransferegovEvent", evento.documentoId());
    }

    @Override
    public void publicarDocumentoRejeitado(DocumentoRejeitadoEvent evento) {
        executarAposCommit(() -> executarPublicacaoDocumentoRejeitado(evento), "DocumentoRejeitadoEvent", evento.documentoId());
    }

    private void executarAposCommit(Runnable acaoEnvio, String nomeEvento, Object documentoId) {
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
            log.debug("Transação ativa detectada. Registrando envio do evento {} (documentoId={}) para AFTER_COMMIT.",
                    nomeEvento, documentoId);
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            try {
                                acaoEnvio.run();
                            } catch (Exception e) {
                                log.error("Falha ao publicar evento {} após commit: documentoId={}", nomeEvento, documentoId, e);
                            }
                        }
                    }
            );
        } else {
            acaoEnvio.run();
        }
    }

    private void executarPublicacaoDocumentoPronto(DocumentoProntoParaTransferegovEvent evento) {
        log.info("Publicando DocumentoProntoParaTransferegovEvent no RabbitMQ: exchange={}, routingKey={}, documentoId={}",
                exchange, routingKeyDocumentoPronto, evento.documentoId());

        rabbitTemplate.convertAndSend(exchange, routingKeyDocumentoPronto, evento, m -> {
            if (evento.tenantId() != null) {
                m.getMessageProperties().setHeader("X-Tenant-Id", evento.tenantId().toString());
            }
            m.getMessageProperties().setHeader("X-Correlation-Id", evento.documentoId().toString());
            return m;
        });
    }

    private void executarPublicacaoDocumentoRejeitado(DocumentoRejeitadoEvent evento) {
        log.info("Publicando DocumentoRejeitadoEvent no RabbitMQ: exchange={}, routingKey={}, documentoId={}",
                exchange, routingKeyDocumentoRejeitado, evento.documentoId());

        rabbitTemplate.convertAndSend(exchange, routingKeyDocumentoRejeitado, evento, m -> {
            if (evento.tenantId() != null) {
                m.getMessageProperties().setHeader("X-Tenant-Id", evento.tenantId().toString());
            }
            m.getMessageProperties().setHeader("X-Correlation-Id", evento.documentoId().toString());
            return m;
        });
    }
}
