package br.com.govflow.core.infrastructure.adapter.in.scheduler;

import br.com.govflow.core.application.port.in.AvaliarConformidadeCaucUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "govflow.cauc.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class CaucDailyScheduler {

    private static final Logger log = LoggerFactory.getLogger(CaucDailyScheduler.class);

    private final AvaliarConformidadeCaucUseCase avaliarConformidadeUseCase;

    public CaucDailyScheduler(AvaliarConformidadeCaucUseCase avaliarConformidadeUseCase) {
        this.avaliarConformidadeUseCase = avaliarConformidadeUseCase;
    }

    @Scheduled(cron = "${govflow.cauc.scheduler.cron:0 0 7 * * *}")
    public void executarVarreduraMatinalCauc() {
        log.info("Iniciando varredura matinal preventiva de regularidade fiscal do CAUC...");
        try {
            var resultado = avaliarConformidadeUseCase.avaliarTodasPrefeiturasGlobal();
            log.info("Varredura matinal concluída com sucesso: {} prefeituras, {} certidões, {} em alerta, {} vencidas, {} alertas emitidos, {} prefeituras bloqueadas.",
                    resultado.totalPrefeiturasAvaliadas(),
                    resultado.totalCertidoesAvaliadas(),
                    resultado.totalCertidoesEmAlerta(),
                    resultado.totalCertidoesVencidas(),
                    resultado.totalAlertasDisparados(),
                    resultado.prefeiturasBloqueadas());
        } catch (Exception e) {
            log.error("Erro durante a execução da varredura matinal do CAUC", e);
        }
    }
}
