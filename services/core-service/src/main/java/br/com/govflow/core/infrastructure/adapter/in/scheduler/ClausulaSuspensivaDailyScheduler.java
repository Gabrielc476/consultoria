package br.com.govflow.core.infrastructure.adapter.in.scheduler;

import br.com.govflow.core.application.port.in.ConsultarClausulaSuspensivaUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "govflow.clausula-suspensiva.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class ClausulaSuspensivaDailyScheduler {

    private static final Logger log = LoggerFactory.getLogger(ClausulaSuspensivaDailyScheduler.class);

    private final ConsultarClausulaSuspensivaUseCase consultarUseCase;

    public ClausulaSuspensivaDailyScheduler(ConsultarClausulaSuspensivaUseCase consultarUseCase) {
        this.consultarUseCase = consultarUseCase;
    }

    @Scheduled(cron = "${govflow.clausula-suspensiva.scheduler.cron:0 0 7 * * *}")
    public void executarVarreduraMatinalPrazosSuspensiva() {
        log.info("Iniciando varredura matinal preventiva de prazos fatais da Cláusula Suspensiva (Caixa GIGOV)...");
        try {
            int alertas = consultarUseCase.executarVarreduraAlertasPrazo();
            log.info("Varredura matinal de cláusula suspensiva concluída com sucesso. Alertas emitidos: {}", alertas);
        } catch (Exception e) {
            log.error("Erro durante a execução da varredura matinal de prazos da cláusula suspensiva", e);
        }
    }
}
