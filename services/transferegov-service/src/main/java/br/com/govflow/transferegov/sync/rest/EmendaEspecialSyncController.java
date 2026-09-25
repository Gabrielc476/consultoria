package br.com.govflow.transferegov.sync.rest;

import br.com.govflow.transferegov.persistence.entity.SincronizacaoLogEntity;
import br.com.govflow.transferegov.sync.pipeline.EmendasEspeciaisSyncPipeline;
import br.com.govflow.transferegov.sync.rest.dto.SyncTriggerResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * Controller de Ingestão e Mutação de Emendas Especiais (Write Side / CQRS-Light).
 * Isola comandos de disparo de sincronização da API federal de transferências especiais.
 */
@RestController
@RequestMapping("/api/v1/transferegov/emendas-especiais")
@Tag(name = "Emendas Especiais (Pix) - Ingestão", description = "Endpoints de comando e sincronização de dados abertos federais de transferências especiais")
public class EmendaEspecialSyncController {

    private final EmendasEspeciaisSyncPipeline syncPipeline;

    public EmendaEspecialSyncController(EmendasEspeciaisSyncPipeline syncPipeline) {
        this.syncPipeline = syncPipeline;
    }

    @PostMapping("/sincronizar")
    @Operation(summary = "Dispara a sincronização manual dos dados de Emendas Especiais a partir da API federal")
    public ResponseEntity<SyncTriggerResponse> sincronizar(
            @RequestParam(name = "force", defaultValue = "false") boolean force,
            @RequestParam(name = "async", defaultValue = "false") boolean async
    ) {
        if (async) {
            CompletableFuture.runAsync(() -> syncPipeline.executeSync(force));
            return ResponseEntity.accepted().body(new SyncTriggerResponse(
                    null,
                    "INICIADO_ASSINCRONO",
                    "Pipeline de sincronização de Emendas Especiais iniciado em segundo plano.",
                    OffsetDateTime.now()
            ));
        }

        SincronizacaoLogEntity result = syncPipeline.executeSync(force);
        return ResponseEntity.ok(new SyncTriggerResponse(
                result != null ? result.getId() : null,
                result != null ? result.getStatus() : "DESCONHECIDO",
                "Sincronização de Emendas Especiais finalizada com status: " + (result != null ? result.getStatus() : "N/A"),
                OffsetDateTime.now()
        ));
    }
}
