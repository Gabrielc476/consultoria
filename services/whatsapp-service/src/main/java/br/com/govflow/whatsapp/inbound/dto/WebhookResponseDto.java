package br.com.govflow.whatsapp.inbound.dto;

public record WebhookResponseDto(
        String status,
        String externalMessageId,
        String message
) {
    public static WebhookResponseDto received(String externalMessageId) {
        return new WebhookResponseDto("RECEIVED", externalMessageId, "Webhook processado e enfileirado com sucesso");
    }

    public static WebhookResponseDto ignored(String reason) {
        return new WebhookResponseDto("IGNORED", null, reason);
    }

    public static WebhookResponseDto alreadyExists(String externalMessageId) {
        return new WebhookResponseDto("ALREADY_EXISTS", externalMessageId, "Mensagem já processada anteriormente (idempotência)");
    }
}
