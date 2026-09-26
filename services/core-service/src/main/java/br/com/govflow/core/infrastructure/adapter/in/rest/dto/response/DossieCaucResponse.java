package br.com.govflow.core.infrastructure.adapter.in.rest.dto.response;

import br.com.govflow.core.application.port.in.ConsultarCaucUseCase.DossieCaucDto;
import br.com.govflow.core.domain.model.StatusCauc;

import java.util.List;
import java.util.UUID;

public record DossieCaucResponse(
        UUID prefeituraId,
        String nomeMunicipio,
        String uf,
        String cnpj,
        StatusCauc statusGeral,
        int certidoesRegulares,
        int certidoesAlerta,
        int certidoesVencidas,
        List<CertidaoCaucResponse> certidoes
) {
    public static DossieCaucResponse fromDto(DossieCaucDto dto) {
        if (dto == null) return null;
        List<CertidaoCaucResponse> items = dto.certidoes() != null
                ? dto.certidoes().stream().map(CertidaoCaucResponse::fromDto).toList()
                : List.of();

        return new DossieCaucResponse(
                dto.prefeituraId(),
                dto.nomeMunicipio(),
                dto.uf(),
                dto.cnpj(),
                dto.statusGeral(),
                dto.certidoesRegulares(),
                dto.certidoesAlerta(),
                dto.certidoesVencidas(),
                items
        );
    }
}
