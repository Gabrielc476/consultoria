package br.com.govflow.core.infrastructure.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQCoreConfig {

    @Value("${govflow.rabbitmq.exchange:govflow.events}")
    private String exchangeName;

    @Value("${govflow.rabbitmq.dlx:govflow.events.dlx}")
    private String dlxName;

    @Value("${govflow.rabbitmq.queue-documentos-processados:fila.documentos.processados}")
    private String queueProcessadosName;

    @Value("${govflow.rabbitmq.queue-documentos-processados-dlq:fila.documentos.processados.dlq}")
    private String queueProcessadosDlqName;

    @Value("${govflow.rabbitmq.routing-key-documento-pronto:documento.pronto.transferegov}")
    private String routingKeyDocumentoPronto;

    @Value("${govflow.rabbitmq.routing-key-documento-rejeitado:documento.rejeitado}")
    private String routingKeyDocumentoRejeitado;

    public static final String ROUTING_KEY_DOCUMENTO_EXTRAIDO = "documento.extraido";
    public static final String ROUTING_KEY_DOCUMENTO_EXTRAIDO_DLQ = "documento.extraido.dlq";

    @Bean
    public TopicExchange eventsExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(dlxName, true, false);
    }

    @Bean
    public Queue documentosProcessadosQueue() {
        return QueueBuilder.durable(queueProcessadosName)
                .withArgument("x-dead-letter-exchange", dlxName)
                .withArgument("x-dead-letter-routing-key", ROUTING_KEY_DOCUMENTO_EXTRAIDO_DLQ)
                .build();
    }

    @Bean
    public Queue documentosProcessadosDlq() {
        return QueueBuilder.durable(queueProcessadosDlqName).build();
    }

    @Bean
    public Binding documentosProcessadosBinding(Queue documentosProcessadosQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(documentosProcessadosQueue)
                .to(eventsExchange)
                .with(ROUTING_KEY_DOCUMENTO_EXTRAIDO);
    }

    @Bean
    public Binding documentosProcessadosDlqBinding(Queue documentosProcessadosDlq, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(documentosProcessadosDlq)
                .to(deadLetterExchange)
                .with(ROUTING_KEY_DOCUMENTO_EXTRAIDO_DLQ);
    }

    @Bean
    public MessageConverter jackson2MessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jackson2MessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jackson2MessageConverter);
        return template;
    }
}
