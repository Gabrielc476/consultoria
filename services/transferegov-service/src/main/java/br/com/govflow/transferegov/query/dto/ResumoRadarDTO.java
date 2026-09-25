package br.com.govflow.transferegov.query.dto;

import java.time.LocalDate;

/**
 * Indicadores consolidados do semáforo de criticidade para os cards do dashboard.
 */
public record ResumoRadarDTO(
        long totalMonitorados,
        long totalCriticos,
        long totalAtencao,
        long totalRegulares,
        LocalDate dataReferencia
) {
}
