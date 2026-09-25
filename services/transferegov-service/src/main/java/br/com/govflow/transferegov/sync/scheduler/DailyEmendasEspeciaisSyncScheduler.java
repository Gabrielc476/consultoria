package br.com.govflow.transferegov.sync.scheduler;

import br.com.govflow.transferegov.sync.pipeline.EmendasEspeciaisSyncPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "govflow.transferegov.especiais.scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class DailyEmendasEspeciaisSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(DailyEmendasEspeciaisSyncScheduler.class);

    private final EmendasEspeciaisSyncPipeline pipeline;

    public DailyEmendasEspeciaisSyncScheduler(EmendasEspeciaisSyncPipeline pipeline) {
        this.pipeline = pipeline;
    }

    @Scheduled(cron = "${govflow.transferegov.especiais.cron:0 30 7 * * *}")
    public void runDailyEmendasEspeciaisSync() {
        log.info("Disparando job agendado matinal de ingestão de Emendas Especiais (Pix) e auditoria ADPF 854...");
        pipeline.executeSync(false);
    }
}
