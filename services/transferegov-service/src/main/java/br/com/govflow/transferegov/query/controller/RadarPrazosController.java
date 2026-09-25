package br.com.govflow.transferegov.query.controller;

import br.com.govflow.transferegov.domain.radar.NivelRisco;
import br.com.govflow.transferegov.query.dto.RadarPrazosResponseDTO;
import br.com.govflow.transferegov.query.service.RadarPrazosQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * Controller REST para o Radar Proativo de Prazos Críticos e Alertas de Vigência.
 */
@RestController
@RequestMapping("/api/v1/transferegov/radar-prazos")
@Tag(name = "Radar de Prazos Transferegov", description = "Endpoints para monitoramento de risco, semáforo de criticidade e alertas de vigência")
public class RadarPrazosController {

    private final RadarPrazosQueryService radarPrazosQueryService;

    public RadarPrazosController(RadarPrazosQueryService radarPrazosQueryService) {
        this.radarPrazosQueryService = radarPrazosQueryService;
    }

    @GetMapping
    @Operation(summary = "Retorna o panorama consolidado do Radar de Prazos com semáforo, agrupamento por município e alertas detalhados")
    public ResponseEntity<RadarPrazosResponseDTO> obterRadar(
            @RequestParam(name = "uf", required = false) String uf,
            @RequestParam(name = "cnpj", required = false) String cnpj,
            @RequestParam(name = "municipio", required = false) String municipio,
            @RequestParam(name = "nivelRisco", required = false) NivelRisco nivelRisco,
            @RequestParam(name = "dataReferencia", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataReferencia
    ) {
        RadarPrazosResponseDTO response = radarPrazosQueryService.obterRadar(
                uf,
                cnpj,
                municipio,
                nivelRisco,
                dataReferencia
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/avaliar")
    @Operation(summary = "Dispara manualmente a avaliação da régua de prazos e despacha eventos no RabbitMQ para convênios críticos")
    public ResponseEntity<Map<String, Object>> dispararAvaliacaoPrazos(
            @RequestParam(name = "dataReferencia", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataReferencia
    ) {
        LocalDate ref = dataReferencia != null ? dataReferencia : LocalDate.now();
        int emitidos = radarPrazosQueryService.avaliarPrazosEAlertar(ref);

        return ResponseEntity.ok(Map.of(
                "status", "SUCESSO",
                "dataReferencia", ref.toString(),
                "totalAlertasCriticosEmitidos", emitidos,
                "exchange", "govflow.events",
                "routingKey", "transferegov.prazo.alerta"
        ));
    }
}
