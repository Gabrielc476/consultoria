package br.com.govflow.whatsapp.chat.dto;

import java.time.Instant;
import java.util.UUID;

public record MensagemChatDto(
        UUID id,
        String externalMessageId,
        String senderPhone,
        String senderName,
        String remetente, // 'CONTATO', 'USUARIO', 'BOT_IA'
        boolean fromMe,
        String messageType, // 'TEXT', 'AUDIO', 'DOCUMENT', 'IMAGE'
        String texto,
        String audioTranscription,
        String s3Key,
        String mediaUrl,
        String mediaMimeType,
        String fileName,
        Long fileSizeBytes,
        Instant dataHora,
        String status
) {}
