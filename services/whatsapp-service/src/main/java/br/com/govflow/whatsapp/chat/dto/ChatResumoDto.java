package br.com.govflow.whatsapp.chat.dto;

import java.time.Instant;

public record ChatResumoDto(
        String remoteJid,
        String phone,
        String name,
        String profilePicUrl,
        String lastMessage,
        Instant lastMessageTime,
        boolean isCadastrado
) {}
