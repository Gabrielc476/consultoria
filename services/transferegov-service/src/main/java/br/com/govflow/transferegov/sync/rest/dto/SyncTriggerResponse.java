package br.com.govflow.transferegov.sync.rest.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SyncTriggerResponse(
        UUID logId,
        String status,
        String message,
        OffsetDateTime triggeredAt
) {}
