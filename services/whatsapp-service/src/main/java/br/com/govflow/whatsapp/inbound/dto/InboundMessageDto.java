package br.com.govflow.whatsapp.inbound.dto;

public record InboundMessageDto(
        String instanceName,
        String externalMessageId,
        String senderPhone,
        String senderName,
        String messageType,
        String contentText,
        String mediaUrl,
        String mediaMimeType,
        String fileName,
        Long fileSizeBytes,
        String rawPayload,
        boolean fromMe
) {
    public boolean isMedia() {
        return "DOCUMENT".equalsIgnoreCase(messageType)
                || "AUDIO".equalsIgnoreCase(messageType)
                || "IMAGE".equalsIgnoreCase(messageType);
    }

    public boolean isDocument() {
        return "DOCUMENT".equalsIgnoreCase(messageType) || "IMAGE".equalsIgnoreCase(messageType);
    }

    public boolean isAudio() {
        return "AUDIO".equalsIgnoreCase(messageType);
    }
}
