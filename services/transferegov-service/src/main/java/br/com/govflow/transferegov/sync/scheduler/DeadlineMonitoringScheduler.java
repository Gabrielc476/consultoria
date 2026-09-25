package br.com.govflow.transferegov.sync.scheduler;

import br.com.govflow.transferegov.query.service.RadarPrazosQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Agendador diário responsável por avaliar os prazos dos convênios monitorados
 * e emitir notificações automáticas de risco crítico no RabbitMQ.
 */
@Component
public class DeadlineMonitoringScheduler {

    private static final Logger log = LoggerFactory.getLogger(DeadlineMonitoringScheduler.class);

    private final RadarPrazosQueryService radarPrazosQueryService;

    public DeadlineMonitoringScheduler(RadarPrazosQueryService radarPrazosQueryService) {
        this.radarPrazosQueryService = radarPrazosQueryService;
    }

    @Scheduled(cron = "${govflow.transferegov.radar.cron:0 30 7 * * *}")
    public void runDailyDeadlineCheck() {
        log.info("Disparando rotina diária matinal de monitoramento de prazos críticos do Transferegov...");
        try {
            int totalEmitidos = radarPrazosQueryService.avaliarPrazosEAlertar(LocalDate.now());
            log.info("Rotina diária de prazos finalizada. Total de alertas críticos expedidos: {}", totalEmitidos);
        } catch (Exception e) {
            log.error("Erro ao executar monitoramento diário de prazos: {}", e.getMessage(), e);
        }
    }
}
