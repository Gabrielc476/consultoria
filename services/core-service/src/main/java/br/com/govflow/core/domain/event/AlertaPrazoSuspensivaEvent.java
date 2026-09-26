package br.com.govflow.core.domain.event;

import br.com.govflow.core.domain.model.convenio.CriticidadePrazoSuspensiva;

import java.util.UUID;

/**
 * Evento de alerta de proximidade do prazo fatal de 180 dias da Cláusula Suspensiva.
 */
public record AlertaPrazoSuspensivaEvent(
        UUID tenantId,
        UUID convenioId,
        String numeroSiconv,
        long diasRestantes,
        CriticidadePrazoSuspensiva criticidade
) {
}
