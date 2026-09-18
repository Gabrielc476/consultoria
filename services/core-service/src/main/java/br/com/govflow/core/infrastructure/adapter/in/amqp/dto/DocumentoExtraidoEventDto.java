package br.com.govflow.core.infrastructure.adapter.in.amqp.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DocumentoExtraidoEventDto(
        @JsonProperty("eventId")
        @JsonAlias("event_id")
        UUID eventId,

        @JsonProperty("eventType")
        @JsonAlias("event_type")
        String eventType,

        @JsonProperty("eventVersion")
        @JsonAlias("event_version")
        String eventVersion,

        @JsonProperty("occurredAt")
        @JsonAlias("occurred_at")
        Instant occurredAt,

        @JsonProperty("tenantId")
        @JsonAlias("tenant_id")
        UUID tenantId,

        @JsonProperty("correlationId")
        @JsonAlias("correlation_id")
        UUID correlationId,

        @JsonProperty("payload")
        DocumentoExtraidoPayloadDto payload
) {
}
