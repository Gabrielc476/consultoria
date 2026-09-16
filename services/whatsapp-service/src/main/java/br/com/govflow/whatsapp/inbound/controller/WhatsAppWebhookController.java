package br.com.govflow.whatsapp.inbound.controller;

import br.com.govflow.whatsapp.inbound.dto.WebhookResponseDto;
import br.com.govflow.whatsapp.inbound.service.InboundMessageProcessor;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/whatsapp")
@Tag(name = "WhatsApp Webhook", description = "Recepção de notificações e mídias de provedores de WhatsApp")
public class WhatsAppWebhookController {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppWebhookController.class);

    private final InboundMessageProcessor inboundMessageProcessor;

    public WhatsAppWebhookController(InboundMessageProcessor inboundMessageProcessor) {
        this.inboundMessageProcessor = inboundMessageProcessor;
    }

    @PostMapping("/webhook")
    @Operation(summary = "Recebe webhooks de mensagens, anexos e áudios (Evolution API / Meta Cloud)")
    public ResponseEntity<WebhookResponseDto> handleWebhook(@RequestBody JsonNode payload) {
        log.debug("Recebendo requisição de webhook no WhatsApp Service");

        WebhookResponseDto response = inboundMessageProcessor.processWebhook(payload);

        if ("RECEIVED".equalsIgnoreCase(response.status())) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        }

        return ResponseEntity.ok(response);
    }
}
