package br.com.govflow.transferegov.query.dto;

import br.com.govflow.transferegov.domain.compliance.StatusAdpf854;
import br.com.govflow.transferegov.persistence.entity.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record EmendaEspecialDetalheDTO(
        UUID id,
        Long idPlanoAcao,
        String codigoPlanoAcao,
        Integer anoPlanoAcao,
        String modalidadePlanoAcao,
        String situacaoPlanoAcao,
        LocalDate dataAceitePlanoAcao,
        String cnpjBeneficiario,
        String nomeBeneficiario,
        String ufBeneficiario,
        Long idBeneficiario,
        String nomeParlamentar,
        Integer anoEmenda,
        Integer numeroEmenda,
        String codigoEmendaFormatado,
        String categoriaDespesa,
        BigDecimal valorCusteio,
        BigDecimal valorInvestimento,
        BigDecimal valorTotal,
        String nomeObjeto,
        String detalhamentoObjeto,
        String areaPoliticaPublica,
        String motivoImpedimento,
        DadosBancariosDTO dadosBancarios,
        StatusAdpf854 statusAdpf854,
        List<PlanoTrabalhoDTO> planosTrabalho,
        List<RelatorioGestaoDTO> relatoriosGestao,
        List<InconformidadeDTO> inconformidades,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public record DadosBancariosDTO(
            String codigoBanco,
            String nomeBanco,
            String numeroAgencia,
            String dvAgencia,
            String numeroConta,
            String dvConta,
            String situacaoDadoBancario
    ) {}

    public record PlanoTrabalhoDTO(
            UUID id,
            Long idPlanoTrabalho,
            String situacaoPlanoTrabalho,
            LocalDate dataInicioExecucao,
            LocalDate dataFimExecucao,
            Integer prazoExecucaoMeses,
            OffsetDateTime dataAprovacao,
            String indOrgaoAnalisesPendentes,
            String classificacaoOrcamentaria,
            String justificativaProrrogacao
    ) {
        public static PlanoTrabalhoDTO fromEntity(EmendaEspecialPlanoTrabalhoEntity e) {
            return new PlanoTrabalhoDTO(
                    e.getId(),
                    e.getIdPlanoTrabalho(),
                    e.getSituacaoPlanoTrabalho(),
                    e.getDataInicioExecucao(),
                    e.getDataFimExecucao(),
                    e.getPrazoExecucaoMeses(),
                    e.getDataAprovacao(),
                    e.getIndOrgaoAnalisesPendentes(),
                    e.getClassificacaoOrcamentaria(),
                    e.getJustificativaProrrogacao()
            );
        }
    }

    public record RelatorioGestaoDTO(
            UUID id,
            Long idRelatorioGestaoNovo,
            String tipoRelatorio,
            String situacaoRelatorio,
            LocalDate dataRelatorio,
            BigDecimal valorExecutado,
            BigDecimal valorPendente
    ) {
        public static RelatorioGestaoDTO fromEntity(EmendaEspecialRelatorioGestaoEntity e) {
            return new RelatorioGestaoDTO(
                    e.getId(),
                    e.getIdRelatorioGestaoNovo(),
                    e.getTipoRelatorio(),
                    e.getSituacaoRelatorio(),
                    e.getDataRelatorio(),
                    e.getValorExecutado(),
                    e.getValorPendente()
            );
        }
    }

    public record InconformidadeDTO(
            UUID id,
            String tipoInconformidade,
            String severidade,
            String descricao,
            boolean resolvido,
            OffsetDateTime dataDeteccao
    ) {
        public static InconformidadeDTO fromEntity(EmendaEspecialInconformidadeEntity e) {
            return new InconformidadeDTO(
                    e.getId(),
                    e.getTipoInconformidade() != null ? e.getTipoInconformidade().name() : null,
                    e.getSeveridade() != null ? e.getSeveridade().name() : null,
                    e.getDescricao(),
                    e.isResolvido(),
                    e.getDataDeteccao()
            );
        }
    }

    public static EmendaEspecialDetalheDTO fromEntity(EmendaEspecialPlanoAcaoEntity entity) {
        if (entity == null) return null;

        DadosBancariosDTO dadosBancarios = new DadosBancariosDTO(
                entity.getCodigoBanco(),
                entity.getNomeBanco(),
                entity.getNumeroAgencia(),
                entity.getDvAgencia(),
                entity.getNumeroConta(),
                entity.getDvConta(),
                entity.getSituacaoDadoBancario()
        );

        List<PlanoTrabalhoDTO> pts = entity.getPlanosTrabalho() != null
                ? entity.getPlanosTrabalho().stream().map(PlanoTrabalhoDTO::fromEntity).toList()
                : List.of();

        List<RelatorioGestaoDTO> rgs = entity.getRelatoriosGestao() != null
                ? entity.getRelatoriosGestao().stream().map(RelatorioGestaoDTO::fromEntity).toList()
                : List.of();

        List<InconformidadeDTO> inconf = entity.getInconformidades() != null
                ? entity.getInconformidades().stream().map(InconformidadeDTO::fromEntity).toList()
                : List.of();

        return new EmendaEspecialDetalheDTO(
                entity.getId(),
                entity.getIdPlanoAcao(),
                entity.getCodigoPlanoAcao(),
                entity.getAnoPlanoAcao(),
                entity.getModalidadePlanoAcao(),
                entity.getSituacaoPlanoAcao(),
                entity.getDataAceitePlanoAcao(),
                entity.getCnpjBeneficiario(),
                entity.getNomeBeneficiario(),
                entity.getUfBeneficiario(),
                entity.getIdBeneficiario(),
                entity.getNomeParlamentar(),
                entity.getAnoEmenda(),
                entity.getNumeroEmenda(),
                entity.getCodigoEmendaFormatado(),
                entity.getCategoriaDespesa(),
                entity.getValorCusteio(),
                entity.getValorInvestimento(),
                entity.getValorTotal(),
                entity.getNomeObjeto(),
                entity.getDetalhamentoObjeto(),
                entity.getAreaPoliticaPublica(),
                entity.getMotivoImpedimento(),
                dadosBancarios,
                entity.getStatusAdpf854(),
                pts,
                rgs,
                inconf,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
