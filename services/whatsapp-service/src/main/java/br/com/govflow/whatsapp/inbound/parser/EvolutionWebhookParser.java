package br.com.govflow.whatsapp.inbound.parser;

import br.com.govflow.whatsapp.inbound.dto.InboundMessageDto;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class EvolutionWebhookParser implements WebhookParserStrategy {

    private static final Logger log = LoggerFactory.getLogger(EvolutionWebhookParser.class);
    private static final String PROVIDER_NAME = "EVOLUTION";

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean supports(String providerName, JsonNode rootNode) {
        if (PROVIDER_NAME.equalsIgnoreCase(providerName)) {
            return true;
        }
        // Auto-detect if payload looks like Evolution API v2
        return rootNode.has("event") && (
                "messages.upsert".equalsIgnoreCase(rootNode.path("event").asText())
                        || "MESSAGES_UPSERT".equalsIgnoreCase(rootNode.path("event").asText())
        );
    }

    @Override
    public Optional<InboundMessageDto> parse(JsonNode rootNode) {
        if (rootNode == null || rootNode.isEmpty()) {
            return Optional.empty();
        }

        // 1. Filtrar eventos que não sejam de mensagens (ex: presence.update, connection.update)
        String event = rootNode.path("event").asText("");
        if (!event.isEmpty() && !event.equalsIgnoreCase("messages.upsert") && !event.equalsIgnoreCase("MESSAGES_UPSERT")) {
            log.debug("Ignorando evento da Evolution API que não é messages.upsert: {}", event);
            return Optional.empty();
        }

        JsonNode dataNode = rootNode.path("data");
        if (dataNode.isMissingNode() || dataNode.isNull()) {
            dataNode = rootNode;
        }

        JsonNode keyNode = dataNode.path("key");
        if (keyNode.isMissingNode()) {
            log.warn("Payload recebido sem nó 'key'. Ignorando processamento.");
            return Optional.empty();
        }

        // 2. Ignorar mensagens enviadas pelo próprio bot/instância
        boolean fromMe = keyNode.path("fromMe").asBoolean(false);
        if (fromMe) {
            log.debug("Ignorando mensagem enviada pelo próprio número (fromMe=true)");
            return Optional.empty();
        }

        String externalMessageId = keyNode.path("id").asText(null);
        if (externalMessageId == null || externalMessageId.isBlank()) {
            log.warn("Mensagem sem ID externo ('key.id'). Ignorando.");
            return Optional.empty();
        }

        String remoteJid = keyNode.path("remoteJid").asText("");
        String senderPhone = extractCleanPhoneNumber(remoteJid);
        String senderName = dataNode.path("pushName").asText(null);
        String instanceName = rootNode.path("instance").asText("govflow-consultoria");

        JsonNode messageNode = dataNode.path("message");
        if (messageNode.isMissingNode() || messageNode.isNull()) {
            log.debug("Mensagem sem nó 'message' (possível atualização de status ou recibo)");
            return Optional.empty();
        }

        String messageType = "TEXT";
        String contentText = null;
        String mediaUrl = null;
        String mediaMimeType = null;
        String fileName = null;
        Long fileSizeBytes = null;

        if (messageNode.has("documentMessage")) {
            JsonNode doc = messageNode.path("documentMessage");
            messageType = "DOCUMENT";
            contentText = doc.path("caption").asText(null);
            mediaUrl = extractMediaUrl(doc, dataNode);
            mediaMimeType = doc.path("mimetype").asText("application/pdf");
            fileName = doc.path("fileName").asText(doc.path("title").asText("documento.pdf"));
            fileSizeBytes = doc.path("fileLength").asLong(0L);
        } else if (messageNode.has("audioMessage")) {
            JsonNode audio = messageNode.path("audioMessage");
            messageType = "AUDIO";
            mediaUrl = extractMediaUrl(audio, dataNode);
            mediaMimeType = audio.path("mimetype").asText("audio/ogg");
            fileName = "audio_" + externalMessageId + ".ogg";
            fileSizeBytes = audio.path("fileLength").asLong(0L);
        } else if (messageNode.has("imageMessage")) {
            JsonNode img = messageNode.path("imageMessage");
            messageType = "IMAGE";
            contentText = img.path("caption").asText(null);
            mediaUrl = extractMediaUrl(img, dataNode);
            mediaMimeType = img.path("mimetype").asText("image/jpeg");
            fileName = "imagem_" + externalMessageId + ".jpg";
            fileSizeBytes = img.path("fileLength").asLong(0L);
        } else if (messageNode.has("conversation")) {
            messageType = "TEXT";
            contentText = messageNode.path("conversation").asText();
        } else if (messageNode.has("extendedTextMessage")) {
            messageType = "TEXT";
            contentText = messageNode.path("extendedTextMessage").path("text").asText();
        } else {
            // Outro tipo de mensagem desconhecido ou não suportado no MVP
            log.debug("Tipo de mensagem não tratado: {}", messageNode.fieldNames());
            contentText = messageNode.toString();
        }

        String rawPayloadString = rootNode.toString();

        return Optional.of(new InboundMessageDto(
                instanceName,
                externalMessageId,
                senderPhone,
                senderName,
                messageType,
                contentText,
                mediaUrl,
                mediaMimeType,
                fileName,
                fileSizeBytes,
                rawPayloadString,
                fromMe
        ));
    }

    private String extractCleanPhoneNumber(String remoteJid) {
        if (remoteJid == null || remoteJid.isBlank()) {
            return "";
        }
        // Remove sufixos como @s.whatsapp.net ou @g.us e remove ":" de instâncias multi-device
        String phonePart = remoteJid.split("@")[0].split(":")[0];
        return phonePart.replaceAll("[^0-9]", "");
    }

    private String extractMediaUrl(JsonNode specificMediaNode, JsonNode dataNode) {
        if (specificMediaNode.hasNonNull("url") && !specificMediaNode.path("url").asText().isBlank()) {
            return specificMediaNode.path("url").asText();
        }
        if (specificMediaNode.hasNonNull("mediaUrl") && !specificMediaNode.path("mediaUrl").asText().isBlank()) {
            return specificMediaNode.path("mediaUrl").asText();
        }
        if (dataNode.hasNonNull("mediaUrl") && !dataNode.path("mediaUrl").asText().isBlank()) {
            return dataNode.path("mediaUrl").asText();
        }
        if (dataNode.hasNonNull("url") && !dataNode.path("url").asText().isBlank()) {
            return dataNode.path("url").asText();
        }
        return null;
    }
}
