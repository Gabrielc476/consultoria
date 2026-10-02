package br.com.govflow.whatsapp.chat.dto;

import jakarta.validation.constraints.NotBlank;

public record EnviarMensagemRequest(
        @NotBlank(message = "O texto da mensagem é obrigatório")
        String texto
) {}
