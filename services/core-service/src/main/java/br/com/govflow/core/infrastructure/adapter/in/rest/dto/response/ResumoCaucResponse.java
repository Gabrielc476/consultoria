package br.com.govflow.core.infrastructure.adapter.in.rest.dto.response;

import br.com.govflow.core.application.port.in.ConsultarCaucUseCase.ResumoCaucDto;

import java.util.List;

public record ResumoCaucResponse(
        int totalMunicipios,
        int totalRegulares,
        int totalAlerta,
        int totalVencidas,
        List<MunicipioRiscoCaucResponse> municipios
) {
    public static ResumoCaucResponse fromDto(ResumoCaucDto dto) {
        if (dto == null) return null;
        List<MunicipioRiscoCaucResponse> muns = dto.municipios() != null
                ? dto.municipios().stream().map(MunicipioRiscoCaucResponse::fromDto).toList()
                : List.of();

        return new ResumoCaucResponse(
                dto.totalMunicipios(),
                dto.totalRegulares(),
                dto.totalAlerta(),
                dto.totalVencidas(),
                muns
        );
    }
}
