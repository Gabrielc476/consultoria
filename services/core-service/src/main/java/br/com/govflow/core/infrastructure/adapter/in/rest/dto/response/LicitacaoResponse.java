package br.com.govflow.core.infrastructure.adapter.in.rest.dto.response;

import br.com.govflow.core.domain.model.licitacao.CriterioJulgamento;
import br.com.govflow.core.domain.model.licitacao.Licitacao;
import br.com.govflow.core.domain.model.licitacao.ModalidadeLicitacao;
import br.com.govflow.core.domain.model.licitacao.RegimeExecucao;
import br.com.govflow.core.domain.model.licitacao.SituacaoLicitacao;
import br.com.govflow.core.domain.model.licitacao.StatusAio;
import br.com.govflow.core.domain.model.licitacao.StatusVrpl;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record LicitacaoResponse(
        UUID id,
        UUID tenantId,
        UUID convenioId,
        UUID prefeituraId,
        String numeroProcessoAdministrativo,
        String numeroLicitacao,
        ModalidadeLicitacao modalidade,
        CriterioJulgamento criterioJulgamento,
        RegimeExecucao regimeExecucao,
        String objeto,
        BigDecimal valorEstimado,
        BigDecimal valorHomologado,
        BigDecimal percentualDesconto,
        BigDecimal valorEconomia,
        SituacaoLicitacao situacao,
        LocalDate dataPublicacaoEdital,
        LocalDate dataAberturaPropostas,
        LocalDate dataHomologacao,
        String linkPncp,
        String linkTransferegov,
        String linkSistemaCompras,
        String numeroVrplTransferegov,
        StatusVrpl statusVrpl,
        LocalDate dataEnvioVrpl,
        LocalDate dataAceiteVrpl,
        String parecerTecnicoVrpl,
        String observacoesDiligenciaVrpl,
        String numeroAio,
        LocalDate dataEmissaoAio,
        StatusAio statusAio,
        String cnpjVencedor,
        String razaoSocialVencedor,
        String s3KeyEdital,
        String s3KeyTermoHomologacao,
        String s3KeyPropostaVencedora,
        String s3KeyAtaSessao,
        String s3KeyParecerVrpl,
        String s3KeyAutorizacaoAio,
        @Schema(description = "True somente com AIO EMITIDO — TravaAntigrosa liberada")
        boolean podeCriarMedicao,
        Instant createdAt,
        Instant updatedAt
) {
    public static LicitacaoResponse fromDomain(Licitacao licitacao) {
        return new LicitacaoResponse(
                licitacao.getId(),
                licitacao.getTenantId(),
                licitacao.getConvenioId(),
                licitacao.getPrefeituraId(),
                licitacao.getNumeroProcessoAdministrativo(),
                licitacao.getNumeroLicitacao(),
                licitacao.getModalidade(),
                licitacao.getCriterioJulgamento(),
                licitacao.getRegimeExecucao(),
                licitacao.getObjeto(),
                licitacao.getValorEstimado(),
                licitacao.getValorHomologado(),
                licitacao.getPercentualDesconto(),
                licitacao.calcularValorEconomia(),
                licitacao.getSituacao(),
                licitacao.getDataPublicacaoEdital(),
                licitacao.getDataAberturaPropostas(),
                licitacao.getDataHomologacao(),
                licitacao.getLinkPncp(),
                licitacao.getLinkTransferegov(),
                licitacao.getLinkSistemaCompras(),
                licitacao.getNumeroVrplTransferegov(),
                licitacao.getStatusVrpl(),
                licitacao.getDataEnvioVrpl(),
                licitacao.getDataAceiteVrpl(),
                licitacao.getParecerTecnicoVrpl(),
                licitacao.getObservacoesDiligenciaVrpl(),
                licitacao.getNumeroAio(),
                licitacao.getDataEmissaoAio(),
                licitacao.getStatusAio(),
                licitacao.getCnpjVencedor(),
                licitacao.getRazaoSocialVencedor(),
                licitacao.getS3KeyEdital(),
                licitacao.getS3KeyTermoHomologacao(),
                licitacao.getS3KeyPropostaVencedora(),
                licitacao.getS3KeyAtaSessao(),
                licitacao.getS3KeyParecerVrpl(),
                licitacao.getS3KeyAutorizacaoAio(),
                licitacao.podeCriarMedicao(),
                licitacao.getCreatedAt(),
                licitacao.getUpdatedAt()
        );
    }
}
