package br.com.govflow.core.infrastructure.adapter.out.amqp;

import br.com.govflow.core.application.port.out.ClausulaSuspensivaEventPublisherPort;
import br.com.govflow.core.domain.event.AlertaPrazoSuspensivaEvent;
import br.com.govflow.core.domain.event.ClausulaSuspensivaSuperadaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class RabbitMQClausulaSuspensivaEventPublisherAdapter implements ClausulaSuspensivaEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQClausulaSuspensivaEventPublisherAdapter.class);

    private final RabbitTemplate rabbitTemplate;

    @Value("${govflow.rabbitmq.exchange:govflow.events}")
    private String exchange;

    @Value("${govflow.rabbitmq.routing-key-clausula-suspensiva-superada:core.clausula-suspensiva.superada}")
    private String routingKeySuperada;

    @Value("${govflow.rabbitmq.routing-key-clausula-suspensiva-alerta:core.clausula-suspensiva.alerta}")
    private String routingKeyAlerta;

    public RabbitMQClausulaSuspensivaEventPublisherAdapter(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publicarSuperacao(ClausulaSuspensivaSuperadaEvent evento) {
        executarAposCommit(() -> {
            log.info("Publicando ClausulaSuspensivaSuperadaEvent: exchange={}, routingKey={}, convenioId={}, siconv={}",
                    exchange, routingKeySuperada, evento.convenioId(), evento.numeroSiconv());

            rabbitTemplate.convertAndSend(exchange, routingKeySuperada, evento, m -> {
                if (evento.tenantId() != null) {
                    m.getMessageProperties().setHeader("X-Tenant-Id", evento.tenantId().toString());
                }
                m.getMessageProperties().setHeader("X-Correlation-Id", evento.convenioId().toString());
                return m;
            });
        }, "ClausulaSuspensivaSuperadaEvent", evento.convenioId());
    }

    @Override
    public void publicarAlertaPrazo(AlertaPrazoSuspensivaEvent evento) {
        executarAposCommit(() -> {
            log.info("Publicando AlertaPrazoSuspensivaEvent: exchange={}, routingKey={}, convenioId={}, diasRestantes={}",
                    exchange, routingKeyAlerta, evento.convenioId(), evento.diasRestantes());

            rabbitTemplate.convertAndSend(exchange, routingKeyAlerta, evento, m -> {
                if (evento.tenantId() != null) {
                    m.getMessageProperties().setHeader("X-Tenant-Id", evento.tenantId().toString());
                }
                m.getMessageProperties().setHeader("X-Correlation-Id", evento.convenioId().toString());
                return m;
            });
        }, "AlertaPrazoSuspensivaEvent", evento.convenioId());
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
