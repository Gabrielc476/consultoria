package br.com.govflow.gateway.domain.model;

import br.com.govflow.gateway.domain.exception.TenantSuspendedException;
import java.util.UUID;

public record TenantContext(UUID tenantId, String tenantCode, boolean active) {

    public void validateActive() {
        if (!active) {
            throw new TenantSuspendedException("Consultoria com acesso suspenso.");
        }
    }
}
