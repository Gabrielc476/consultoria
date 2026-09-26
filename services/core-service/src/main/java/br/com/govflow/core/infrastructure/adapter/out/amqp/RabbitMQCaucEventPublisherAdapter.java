package br.com.govflow.core.infrastructure.adapter.out.amqp;

import br.com.govflow.core.application.port.out.CaucEventPublisherPort;
import br.com.govflow.core.domain.event.AlertaCertidaoCaucEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class RabbitMQCaucEventPublisherAdapter implements CaucEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQCaucEventPublisherAdapter.class);

    private final RabbitTemplate rabbitTemplate;

    @Value("${govflow.rabbitmq.exchange:govflow.events}")
    private String exchange;

    @Value("${govflow.rabbitmq.routing-key-cauc-alerta:core.cauc.alerta}")
    private String routingKeyCaucAlerta;

    public RabbitMQCaucEventPublisherAdapter(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publicarAlerta(AlertaCertidaoCaucEvent evento) {
        executarAposCommit(() -> executarPublicacao(evento), "AlertaCertidaoCaucEvent", evento.prefeituraId());
    }

    private void executarPublicacao(AlertaCertidaoCaucEvent evento) {
        log.info("Publicando AlertaCertidaoCaucEvent no RabbitMQ: exchange={}, routingKey={}, prefeituraId={}, exigencia={}",
                exchange, routingKeyCaucAlerta, evento.prefeituraId(), evento.codigoExigencia());

        rabbitTemplate.convertAndSend(exchange, routingKeyCaucAlerta, evento, m -> {
            if (evento.tenantId() != null) {
                m.getMessageProperties().setHeader("X-Tenant-Id", evento.tenantId().toString());
            }
            m.getMessageProperties().setHeader("X-Correlation-Id", evento.prefeituraId().toString());
            return m;
        });
    }

    private void executarAposCommit(Runnable acaoEnvio, String nomeEvento, Object id) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        acaoEnvio.run();
                    } catch (Exception e) {
                        log.error("Falha ao publicar evento {} após commit: id={}", nomeEvento, id, e);
                    }
                }
            });
        } else {
            acaoEnvio.run();
        }
    }
}
