package br.com.govflow.whatsapp.outbound.strategy;

import br.com.govflow.whatsapp.outbound.dto.MessageSentResult;

public interface WhatsAppSenderStrategy {

    String getProviderName();

    MessageSentResult sendTextMessage(String toPhoneNumber, String messageText);

    MessageSentResult sendMediaDocument(String toPhoneNumber, String s3FileUrl, String caption);
}
