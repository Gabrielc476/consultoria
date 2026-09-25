package br.com.govflow.transferegov.event.producer;

import br.com.govflow.transferegov.domain.radar.AlertaConvenioDTO;
import br.com.govflow.transferegov.event.model.AlertaPrazoExpirandoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Publicador de eventos de domínio do Transferegov para o broker RabbitMQ.
 */
@Component
public class TransferegovEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(TransferegovEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchangeName;
    private final String routingKeyAlertaPrazo;
    private final String routingKeyAlertaAdpf854;

    public TransferegovEventPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${govflow.transferegov.rabbitmq.exchange:govflow.events}") String exchangeName,
            @Value("${govflow.transferegov.rabbitmq.routing-key-alerta-prazo:transferegov.prazo.alerta}") String routingKeyAlertaPrazo
    ) {
        this(rabbitTemplate, exchangeName, routingKeyAlertaPrazo, "transferegov.emenda.adpf854.alerta");
    }

    @org.springframework.beans.factory.annotation.Autowired
    public TransferegovEventPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${govflow.transferegov.rabbitmq.exchange:govflow.events}") String exchangeName,
            @Value("${govflow.transferegov.rabbitmq.routing-key-alerta-prazo:transferegov.prazo.alerta}") String routingKeyAlertaPrazo,
            @Value("${govflow.transferegov.rabbitmq.routing-key-alerta-adpf854:transferegov.emenda.adpf854.alerta}") String routingKeyAlertaAdpf854
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchangeName = exchangeName;
        this.routingKeyAlertaPrazo = routingKeyAlertaPrazo;
        this.routingKeyAlertaAdpf854 = routingKeyAlertaAdpf854;
    }

    public void publicarAlertaInconformidadeAdpf854(br.com.govflow.transferegov.event.model.AlertaInconformidadeAdpf854Event event) {
        if (event == null) {
            return;
        }

        try {
            log.info("Publicando alerta de inconformidade ADPF 854 no RabbitMQ [Plano: {}, Município: {}, Tipo: {}] na exchange '{}' com rk '{}'",
                    event.codigoPlanoAcao(), event.municipio(), event.tipoInconformidade(), exchangeName, routingKeyAlertaAdpf854);

            rabbitTemplate.convertAndSend(exchangeName, routingKeyAlertaAdpf854, event);
        } catch (Exception e) {
            log.error("Falha ao publicar alerta de inconformidade ADPF 854 para plano {}: {}", event.codigoPlanoAcao(), e.getMessage(), e);
        }
    }

    public void publicarAlertaPrazo(AlertaPrazoExpirandoEvent event) {
        if (event == null) {
            return;
        }

        try {
            log.info("Publicando evento de prazo crítico no RabbitMQ [Convênio: {}, Risco: {}, Dias: {}] na exchange '{}' com rk '{}'",
                    event.nrConvenio(), event.nivelRisco(), event.diasRestantes(), exchangeName, routingKeyAlertaPrazo);

            rabbitTemplate.convertAndSend(exchangeName, routingKeyAlertaPrazo, event);
        } catch (Exception e) {
            log.error("Falha ao publicar evento de alerta de prazo para convênio {}: {}", event.nrConvenio(), e.getMessage(), e);
        }
    }

    public void publicarAlertaPrazoSeCritico(AlertaConvenioDTO alerta) {
        if (alerta == null || alerta.nivelRisco() != br.com.govflow.transferegov.domain.radar.NivelRisco.CRITICO) {
            return;
        }

        AlertaPrazoExpirandoEvent event = AlertaPrazoExpirandoEvent.of(
                alerta.nrConvenio(),
                alerta.municipio(),
                alerta.uf(),
                alerta.cnpjProponente(),
                alerta.nomeProponente(),
                alerta.tipoPrazoMaisProximo(),
                alerta.prazoMaisProximo(),
                alerta.diasRestantes(),
                alerta.nivelRisco(),
                alerta.valorGlobal()
        );

        publicarAlertaPrazo(event);
    }
}
