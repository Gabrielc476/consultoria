package br.com.govflow.core.infrastructure.adapter.in.rest.dto.response;

import br.com.govflow.core.domain.model.convenio.Convenio;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ConvenioResponse(
        @Schema(description = "Identificador único do convênio")
        UUID id,

        @Schema(description = "Identificador do Tenant (Consultoria)")
        UUID tenantId,

        @Schema(description = "Identificador da prefeitura convenente")
        UUID prefeituraId,

        @Schema(description = "Número SICONV / Transferegov")
        String numeroSiconv,

        @Schema(description = "Número do processo administrativo")
        String numeroProcesso,

        @Schema(description = "Órgão concedente")
        String orgaoConcedente,

        @Schema(description = "Objeto do convênio")
        String objeto,

        @Schema(description = "Valor global acordado")
        BigDecimal valorGlobal,

        @Schema(description = "Valor de repasse federal")
        BigDecimal valorRepasse,

        @Schema(description = "Valor da contrapartida municipal")
        BigDecimal valorContrapartida,

        @Schema(description = "Situação da execução")
        String situacao,

        @Schema(description = "Possui cláusula suspensiva ativa")
        boolean possuiClausulaSuspensiva,

        @Schema(description = "Prazo fatal da cláusula suspensiva")
        LocalDate prazoClausulaSuspensiva,

        @Schema(description = "Data de início da vigência")
        LocalDate dataInicioVigencia,

        @Schema(description = "Data de término da vigência")
        LocalDate dataFimVigencia,

        @Schema(description = "Status operacional da cláusula suspensiva")
        String statusClausulaSuspensiva,

        @Schema(description = "Data/hora de criação no sistema")
        Instant createdAt,

        @Schema(description = "Data/hora da última atualização")
        Instant updatedAt
) {
    public static ConvenioResponse fromDomain(Convenio c) {
        return new ConvenioResponse(
                c.getId(),
                c.getTenantId(),
                c.getPrefeituraId(),
                c.getNumeroSiconv(),
                c.getNumeroProcesso(),
                c.getOrgaoConcedente(),
                c.getObjeto(),
                c.getValorGlobal(),
                c.getValorRepasse(),
                c.getValorContrapartida(),
                c.getSituacao(),
                c.isPossuiClausulaSuspensiva(),
                c.getPrazoFatalEfetivo(),
                c.getDataInicioVigencia(),
                c.getDataFimVigencia(),
                c.getStatusClausulaSuspensiva(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}
