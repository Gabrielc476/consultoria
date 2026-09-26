package br.com.govflow.core.infrastructure.adapter.in.rest.dto.response;

import br.com.govflow.core.application.port.in.ConsultarClausulaSuspensivaUseCase.DossieClausulaSuspensivaDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DossieClausulaSuspensivaResponse(
        UUID convenioId,
        UUID prefeituraId,
        String numeroSiconv,
        String numeroProcesso,
        String orgaoConcedente,
        String objeto,
        BigDecimal valorGlobal,
        BigDecimal valorRepasse,
        BigDecimal valorContrapartida,
        boolean possuiClausulaSuspensiva,
        LocalDate prazoOriginal,
        boolean prorrogacaoSolicitada,
        LocalDate novoPrazoProrrogado,
        LocalDate prazoFatalEfetivo,
        long diasRestantes,
        String criticidade,
        String criticidadeDescricao,
        boolean superada,
        String s3KeyTermoRetirada,
        List<CondicionanteSuspensivaResponse> condicionantes
) {

    public static DossieClausulaSuspensivaResponse fromDto(DossieClausulaSuspensivaDto dto) {
        if (dto == null) return null;

        List<CondicionanteSuspensivaResponse> condicionantesResponse = dto.condicionantes() != null
                ? dto.condicionantes().stream().map(CondicionanteSuspensivaResponse::fromDto).toList()
                : List.of();

        return new DossieClausulaSuspensivaResponse(
                dto.convenioId(),
                dto.prefeituraId(),
                dto.numeroSiconv(),
                dto.numeroProcesso(),
                dto.orgaoConcedente(),
                dto.objeto(),
                dto.valorGlobal(),
                dto.valorRepasse(),
                dto.valorContrapartida(),
                dto.possuiClausulaSuspensiva(),
                dto.prazoOriginal(),
                dto.prorrogacaoSolicitada(),
                dto.novoPrazoProrrogado(),
                dto.prazoFatalEfetivo(),
                dto.diasRestantes(),
                dto.criticidade() != null ? dto.criticidade().name() : null,
                dto.criticidade() != null ? dto.criticidade().getDescricao() : null,
                dto.superada(),
                dto.s3KeyTermoRetirada(),
                condicionantesResponse
        );
    }
}
