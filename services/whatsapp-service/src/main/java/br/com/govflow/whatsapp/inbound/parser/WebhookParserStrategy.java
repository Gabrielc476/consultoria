package br.com.govflow.whatsapp.inbound.parser;

import br.com.govflow.whatsapp.inbound.dto.InboundMessageDto;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Optional;

public interface WebhookParserStrategy {

    String getProviderName();

    boolean supports(String providerName, JsonNode rootNode);

    Optional<InboundMessageDto> parse(JsonNode rootNode);
}
