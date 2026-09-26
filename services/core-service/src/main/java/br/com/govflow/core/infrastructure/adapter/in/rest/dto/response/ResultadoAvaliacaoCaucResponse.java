package br.com.govflow.core.infrastructure.adapter.in.rest.dto.response;

import br.com.govflow.core.application.port.in.AvaliarConformidadeCaucUseCase.ResultadoAvaliacaoDto;

public record ResultadoAvaliacaoCaucResponse(
        int totalPrefeiturasAvaliadas,
        int totalCertidoesAvaliadas,
        int totalCertidoesEmAlerta,
        int totalCertidoesVencidas,
        int totalAlertasDisparados,
        int prefeiturasBloqueadas
) {
    public static ResultadoAvaliacaoCaucResponse fromDto(ResultadoAvaliacaoDto dto) {
        if (dto == null) return null;
        return new ResultadoAvaliacaoCaucResponse(
                dto.totalPrefeiturasAvaliadas(),
                dto.totalCertidoesAvaliadas(),
                dto.totalCertidoesEmAlerta(),
                dto.totalCertidoesVencidas(),
                dto.totalAlertasDisparados(),
                dto.prefeiturasBloqueadas()
        );
    }
}
