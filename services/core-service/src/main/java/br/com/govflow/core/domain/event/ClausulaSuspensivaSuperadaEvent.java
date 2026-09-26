package br.com.govflow.core.domain.event;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Evento de domínio disparado quando todos os 3 pilares da Caixa são aprovados e o Termo de Retirada é registrado.
 */
public record ClausulaSuspensivaSuperadaEvent(
        UUID tenantId,
        UUID convenioId,
        String numeroSiconv,
        LocalDate dataSuperacao,
        String s3KeyTermoRetirada
) {
}
