package br.com.govflow.core.infrastructure.adapter.in.rest.dto.response;

import br.com.govflow.core.application.port.in.ConsultarCaucUseCase.MunicipioRiscoDto;
import br.com.govflow.core.application.port.in.ConsultarCaucUseCase.PrazoFatalDto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record MunicipioRiscoCaucResponse(
        UUID id,
        String nome,
        String uf,
        int conveniosAtivos,
        int certidoesRegulares,
        int certidoesAlerta,
        int certidoesVencidas,
        PrazoFatalResponse proximoPrazoFatal,
        List<CertidaoCaucResponse> certidoes
) {
    public record PrazoFatalResponse(
            String descricao,
            int diasRestantes,
            LocalDate dataLimite,
            String tipo
    ) {
        public static PrazoFatalResponse fromDto(PrazoFatalDto dto) {
            if (dto == null) return null;
            return new PrazoFatalResponse(
                    dto.descricao(),
                    dto.diasRestantes(),
                    dto.dataLimite(),
                    dto.tipo()
            );
        }
    }

    public static MunicipioRiscoCaucResponse fromDto(MunicipioRiscoDto dto) {
        if (dto == null) return null;
        List<CertidaoCaucResponse> certs = dto.certidoes() != null
                ? dto.certidoes().stream().map(CertidaoCaucResponse::fromDto).toList()
                : List.of();

        return new MunicipioRiscoCaucResponse(
                dto.id(),
                dto.nome(),
                dto.uf(),
                dto.conveniosAtivos(),
                dto.certidoesRegulares(),
                dto.certidoesAlerta(),
                dto.certidoesVencidas(),
                PrazoFatalResponse.fromDto(dto.proximoPrazoFatal()),
                certs
        );
    }
}
