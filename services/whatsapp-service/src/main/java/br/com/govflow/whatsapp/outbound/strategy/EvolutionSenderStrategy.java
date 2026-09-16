package br.com.govflow.whatsapp.outbound.strategy;

import br.com.govflow.whatsapp.config.WhatsAppProperties;
import br.com.govflow.whatsapp.outbound.dto.MessageSentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class EvolutionSenderStrategy implements WhatsAppSenderStrategy {

    private static final Logger log = LoggerFactory.getLogger(EvolutionSenderStrategy.class);
    private static final String PROVIDER_NAME = "EVOLUTION";

    private final WhatsAppProperties properties;
    private final HttpClient httpClient;

    public EvolutionSenderStrategy(WhatsAppProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public MessageSentResult sendTextMessage(String toPhoneNumber, String messageText) {
        String instance = properties.getEvolution().getInstanceName();
        String url = String.format("%s/message/sendText/%s", properties.getEvolution().getBaseUrl(), instance);

        log.info("Disparando mensagem de texto para {} via Evolution API ({})", toPhoneNumber, url);

        try {
            String jsonBody = String.format("{\"number\":\"%s\",\"text\":\"%s\"}", toPhoneNumber, escapeJson(messageText));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("apikey", properties.getEvolution().getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return MessageSentResult.ok("EVOLUTION-" + System.currentTimeMillis());
            } else {
                log.error("Erro da Evolution API ao enviar texto. Status: {}, Body: {}", response.statusCode(), response.body());
                return MessageSentResult.failed("Evolution API status " + response.statusCode());
            }
        } catch (Exception e) {
            log.error("Falha de conexão com Evolution API: {}", e.getMessage(), e);
            return MessageSentResult.failed(e.getMessage());
        }
    }

    @Override
    public MessageSentResult sendMediaDocument(String toPhoneNumber, String s3FileUrl, String caption) {
        String instance = properties.getEvolution().getInstanceName();
        String url = String.format("%s/message/sendMedia/%s", properties.getEvolution().getBaseUrl(), instance);

        log.info("Disparando documento para {} via Evolution API ({})", toPhoneNumber, url);

        try {
            String jsonBody = String.format("{\"number\":\"%s\",\"media\":\"%s\",\"caption\":\"%s\"}",
                    toPhoneNumber, s3FileUrl, escapeJson(caption != null ? caption : ""));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("apikey", properties.getEvolution().getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return MessageSentResult.ok("EVOLUTION-MEDIA-" + System.currentTimeMillis());
            } else {
                return MessageSentResult.failed("Evolution API status " + response.statusCode());
            }
        } catch (Exception e) {
            return MessageSentResult.failed(e.getMessage());
        }
    }

    private String escapeJson(String raw) {
        if (raw == null) return "";
        return raw.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
