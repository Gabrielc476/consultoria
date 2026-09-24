package br.com.govflow.transferegov.sync.rest;

import br.com.govflow.transferegov.persistence.entity.SincronizacaoLogEntity;
import br.com.govflow.transferegov.persistence.repository.SincronizacaoAnomaliaRepository;
import br.com.govflow.transferegov.persistence.repository.SincronizacaoConvenioRepository;
import br.com.govflow.transferegov.persistence.repository.SincronizacaoLogRepository;
import br.com.govflow.transferegov.sync.pipeline.SiconvStreamingPipeline;
import br.com.govflow.transferegov.sync.rest.dto.DataQualityMetricsResponse;
import br.com.govflow.transferegov.sync.rest.dto.SyncStatusResponse;
import br.com.govflow.transferegov.sync.rest.dto.SyncTriggerResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * Controller REST do Lado de Ingestão e Sincronização (Write Side / CQRS-Light).
 */
@RestController
@RequestMapping("/api/v1/transferegov/sync")
@Tag(name = "Sincronização SICONV", description = "Endpoints de ingestão por streaming e métricas de qualidade de dados")
public class SincronizacaoController {

    private final SiconvStreamingPipeline pipeline;
    private final SincronizacaoLogRepository logRepository;
    private final SincronizacaoConvenioRepository convenioRepository;
    private final SincronizacaoAnomaliaRepository anomaliaRepository;

    public SincronizacaoController(
            SiconvStreamingPipeline pipeline,
            SincronizacaoLogRepository logRepository,
            SincronizacaoConvenioRepository convenioRepository,
            SincronizacaoAnomaliaRepository anomaliaRepository
    ) {
        this.pipeline = pipeline;
        this.logRepository = logRepository;
        this.convenioRepository = convenioRepository;
        this.anomaliaRepository = anomaliaRepository;
    }

    @PostMapping("/trigger")
    @Operation(summary = "Aciona o pipeline de sincronização dos dumps SICONV em streaming")
    public ResponseEntity<SyncTriggerResponse> triggerSync(
            @RequestParam(name = "force", defaultValue = "false") boolean force,
            @RequestParam(name = "async", defaultValue = "false") boolean async
    ) {
        if (async) {
            CompletableFuture.runAsync(() -> pipeline.executeSync(force));
            return ResponseEntity.accepted().body(new SyncTriggerResponse(
                    null,
                    "INICIADO_ASSINCRONO",
                    "Pipeline de sincronização iniciado em segundo plano.",
                    OffsetDateTime.now()
            ));
        }

        SincronizacaoLogEntity result = pipeline.executeSync(force);
        return ResponseEntity.ok(new SyncTriggerResponse(
                result != null ? result.getId() : null,
                result != null ? result.getStatus() : "DESCONHECIDO",
                "Execução do pipeline finalizada.",
                OffsetDateTime.now()
        ));
    }

    @GetMapping("/status")
    @Operation(summary = "Consulta o status e a telemetria da última sincronização executada")
    public ResponseEntity<SyncStatusResponse> getStatus() {
        return logRepository.findTopByOrderByDataInicioDesc()
                .map(SyncStatusResponse::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/metrics")
    @Operation(summary = "Retorna métricas consolidadas de Data Quality e volumetria de convênios")
    public ResponseEntity<DataQualityMetricsResponse> getMetrics() {
        long totalConvenios = convenioRepository.count();
        long totalPb = convenioRepository.findByUf("PB", Pageable.unpaged()).getTotalElements();
        long totalAnomalias = anomaliaRepository.count();

        SyncStatusResponse lastSync = logRepository.findTopByOrderByDataInicioDesc()
                .map(SyncStatusResponse::fromEntity)
                .orElse(null);

        double taxaSucesso = 100.0;
        if (totalConvenios + totalAnomalias > 0) {
            taxaSucesso = (double) totalConvenios / (totalConvenios + totalAnomalias) * 100.0;
        }

        return ResponseEntity.ok(new DataQualityMetricsResponse(
                totalConvenios,
                totalPb,
                totalAnomalias,
                taxaSucesso,
                lastSync
        ));
    }
}
