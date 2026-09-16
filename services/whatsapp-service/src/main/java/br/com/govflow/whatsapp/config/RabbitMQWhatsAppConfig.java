package br.com.govflow.whatsapp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQWhatsAppConfig {

    public static final String DOCUMENTOS_EXTRAIR_QUEUE = "fila.documentos.extrair";
    public static final String DOCUMENTOS_EXTRAIR_DLQ = "fila.documentos.extrair.dlq";
    public static final String AUDIOS_TRANSCREVER_QUEUE = "fila.audios.transcrever";
    public static final String AUDIOS_TRANSCREVER_DLQ = "fila.audios.transcrever.dlq";

    public static final String ROUTING_KEY_DOCUMENTOS = "whatsapp.documento.recebido";
    public static final String ROUTING_KEY_DOCUMENTOS_DLQ = "whatsapp.documento.extrair.dlq";
    public static final String ROUTING_KEY_AUDIOS = "whatsapp.audio.recebido";
    public static final String ROUTING_KEY_AUDIOS_DLQ = "whatsapp.audio.transcrever.dlq";

    @Bean
    public TopicExchange eventsExchange(WhatsAppProperties properties) {
        return new TopicExchange(properties.getRabbitmq().getExchange(), true, false);
    }

    @Bean
    public TopicExchange deadLetterExchange(WhatsAppProperties properties) {
        return new TopicExchange(properties.getRabbitmq().getDlx(), true, false);
    }

    @Bean
    public Queue documentosExtrairQueue(WhatsAppProperties properties) {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", properties.getRabbitmq().getDlx());
        args.put("x-dead-letter-routing-key", ROUTING_KEY_DOCUMENTOS_DLQ);
        return QueueBuilder.durable(DOCUMENTOS_EXTRAIR_QUEUE).withArguments(args).build();
    }

    @Bean
    public Queue documentosExtrairDlq() {
        return QueueBuilder.durable(DOCUMENTOS_EXTRAIR_DLQ).build();
    }

    @Bean
    public Binding documentosBinding(Queue documentosExtrairQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(documentosExtrairQueue).to(eventsExchange).with(ROUTING_KEY_DOCUMENTOS);
    }

    @Bean
    public Binding documentosDlqBinding(Queue documentosExtrairDlq, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(documentosExtrairDlq).to(deadLetterExchange).with(ROUTING_KEY_DOCUMENTOS_DLQ);
    }

    @Bean
    public Queue audiosTranscreverQueue(WhatsAppProperties properties) {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", properties.getRabbitmq().getDlx());
        args.put("x-dead-letter-routing-key", ROUTING_KEY_AUDIOS_DLQ);
        return QueueBuilder.durable(AUDIOS_TRANSCREVER_QUEUE).withArguments(args).build();
    }

    @Bean
    public Queue audiosTranscreverDlq() {
        return QueueBuilder.durable(AUDIOS_TRANSCREVER_DLQ).build();
    }

    @Bean
    public Binding audiosBinding(Queue audiosTranscreverQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(audiosTranscreverQueue).to(eventsExchange).with(ROUTING_KEY_AUDIOS);
    }

    @Bean
    public Binding audiosDlqBinding(Queue audiosTranscreverDlq, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(audiosTranscreverDlq).to(deadLetterExchange).with(ROUTING_KEY_AUDIOS_DLQ);
    }

    @Bean
    public MessageConverter jackson2MessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jackson2MessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jackson2MessageConverter);
        return template;
    }
}
