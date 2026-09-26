package br.com.govflow.core.application.port.in;

import java.util.UUID;

public interface AvaliarConformidadeCaucUseCase {

    ResultadoAvaliacaoDto avaliarTodasPrefeituras(UUID tenantId);

    ResultadoAvaliacaoDto avaliarTodasPrefeiturasGlobal();

    ResultadoAvaliacaoDto avaliarPrefeitura(UUID prefeituraId);

    record ResultadoAvaliacaoDto(
            int totalPrefeiturasAvaliadas,
            int totalCertidoesAvaliadas,
            int totalCertidoesEmAlerta,
            int totalCertidoesVencidas,
            int totalAlertasDisparados,
            int prefeiturasBloqueadas
    ) {}
}
