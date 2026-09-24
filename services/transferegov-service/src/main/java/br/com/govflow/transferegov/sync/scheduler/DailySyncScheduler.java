package br.com.govflow.transferegov.sync.scheduler;

import br.com.govflow.transferegov.sync.pipeline.SiconvStreamingPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DailySyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(DailySyncScheduler.class);

    private final SiconvStreamingPipeline pipeline;

    public DailySyncScheduler(SiconvStreamingPipeline pipeline) {
        this.pipeline = pipeline;
    }

    @Scheduled(cron = "${govflow.transferegov.siconv.cron:0 0 7 * * *}")
    public void runDailySync() {
        log.info("Disparando job agendado matinal de sincronização de dados abertos do SICONV...");
        pipeline.executeSync(false);
    }
}
