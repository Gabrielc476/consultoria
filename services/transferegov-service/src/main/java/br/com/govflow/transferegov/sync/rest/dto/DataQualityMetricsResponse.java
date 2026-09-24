package br.com.govflow.transferegov.sync.rest.dto;

public record DataQualityMetricsResponse(
        long totalConveniosCadastrados,
        long totalConveniosUfPb,
        long totalAnomaliasRegistradas,
        double taxaQualidadeSucesso,
        SyncStatusResponse ultimaSincronizacao
) {}
