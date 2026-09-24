package br.com.govflow.transferegov.sync.rest.dto;

import br.com.govflow.transferegov.persistence.entity.SincronizacaoLogEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SyncStatusResponse(
        UUID logId,
        String tipoSincronizacao,
        String status,
        OffsetDateTime dataInicio,
        OffsetDateTime dataFim,
        Long tempoExecucaoMs,
        long totalRegistrosLidos,
        long totalRegistrosFiltrados,
        long totalRegistrosPersistidos,
        long totalAnomalias,
        String dataCargaSiconvReferencia,
        String mensagemErro
) {
    public static SyncStatusResponse fromEntity(SincronizacaoLogEntity entity) {
        if (entity == null) return null;
        return new SyncStatusResponse(
                entity.getId(),
                entity.getTipoSincronizacao(),
                entity.getStatus(),
                entity.getDataInicio(),
                entity.getDataFim(),
                entity.getTempoExecucaoMs(),
                entity.getTotalRegistrosLidos(),
                entity.getTotalRegistrosFiltrados(),
                entity.getTotalRegistrosPersistidos(),
                entity.getTotalAnomalias(),
                entity.getDataCargaSiconvReferencia(),
                entity.getMensagemErro()
        );
    }
}
