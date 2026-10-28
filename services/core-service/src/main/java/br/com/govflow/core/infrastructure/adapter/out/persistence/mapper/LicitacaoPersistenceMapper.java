package br.com.govflow.core.infrastructure.adapter.out.persistence.mapper;

import br.com.govflow.core.domain.model.licitacao.CriterioJulgamento;
import br.com.govflow.core.domain.model.licitacao.Licitacao;
import br.com.govflow.core.domain.model.licitacao.ModalidadeLicitacao;
import br.com.govflow.core.domain.model.licitacao.RegimeExecucao;
import br.com.govflow.core.domain.model.licitacao.SituacaoLicitacao;
import br.com.govflow.core.domain.model.licitacao.StatusAio;
import br.com.govflow.core.domain.model.licitacao.StatusVrpl;
import br.com.govflow.core.infrastructure.adapter.out.persistence.entity.LicitacaoJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class LicitacaoPersistenceMapper {

    public Licitacao toDomain(LicitacaoJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Licitacao(
                entity.getId(),
                entity.getTenantId(),
                entity.getConvenioId(),
                entity.getPrefeituraId(),
                entity.getNumeroProcessoAdministrativo(),
                entity.getNumeroLicitacao(),
                enumOrNull(ModalidadeLicitacao.class, entity.getModalidade()),
                enumOrNull(CriterioJulgamento.class, entity.getCriterioJulgamento()),
                enumOrNull(RegimeExecucao.class, entity.getRegimeExecucao()),
                entity.getObjeto(),
                entity.getValorEstimado(),
                entity.getValorHomologado(),
                entity.getPercentualDesconto(),
                enumOrNull(SituacaoLicitacao.class, entity.getSituacao()),
                entity.getDataPublicacaoEdital(),
                entity.getDataAberturaPropostas(),
                entity.getDataHomologacao(),
                entity.getLinkPncp(),
                entity.getLinkTransferegov(),
                entity.getLinkSistemaCompras(),
                entity.getNumeroVrplTransferegov(),
                enumOrNull(StatusVrpl.class, entity.getStatusVrpl()),
                entity.getDataEnvioVrpl(),
                entity.getDataAceiteVrpl(),
                entity.getParecerTecnicoVrpl(),
                entity.getObservacoesDiligenciaVrpl(),
                entity.getNumeroAio(),
                entity.getDataEmissaoAio(),
                enumOrNull(StatusAio.class, entity.getStatusAio()),
                entity.getCnpjVencedor(),
                entity.getRazaoSocialVencedor(),
                entity.getS3KeyEdital(),
                entity.getS3KeyTermoHomologacao(),
                entity.getS3KeyPropostaVencedora(),
                entity.getS3KeyAtaSessao(),
                entity.getS3KeyParecerVrpl(),
                entity.getS3KeyAutorizacaoAio(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public LicitacaoJpaEntity toEntity(Licitacao domain) {
        if (domain == null) {
            return null;
        }
        LicitacaoJpaEntity entity = new LicitacaoJpaEntity();
        entity.setId(domain.getId());
        entity.setTenantId(domain.getTenantId());
        entity.setConvenioId(domain.getConvenioId());
        entity.setPrefeituraId(domain.getPrefeituraId());
        entity.setNumeroProcessoAdministrativo(domain.getNumeroProcessoAdministrativo());
        entity.setNumeroLicitacao(domain.getNumeroLicitacao());
        entity.setModalidade(nameOrNull(domain.getModalidade()));
        entity.setCriterioJulgamento(nameOrNull(domain.getCriterioJulgamento()));
        entity.setRegimeExecucao(nameOrNull(domain.getRegimeExecucao()));
        entity.setObjeto(domain.getObjeto());
        entity.setValorEstimado(domain.getValorEstimado());
        entity.setValorHomologado(domain.getValorHomologado());
        entity.setPercentualDesconto(domain.getPercentualDesconto());
        entity.setSituacao(nameOrNull(domain.getSituacao()));
        entity.setDataPublicacaoEdital(domain.getDataPublicacaoEdital());
        entity.setDataAberturaPropostas(domain.getDataAberturaPropostas());
        entity.setDataHomologacao(domain.getDataHomologacao());
        entity.setLinkPncp(domain.getLinkPncp());
        entity.setLinkTransferegov(domain.getLinkTransferegov());
        entity.setLinkSistemaCompras(domain.getLinkSistemaCompras());
        entity.setNumeroVrplTransferegov(domain.getNumeroVrplTransferegov());
        entity.setStatusVrpl(nameOrNull(domain.getStatusVrpl()));
        entity.setDataEnvioVrpl(domain.getDataEnvioVrpl());
        entity.setDataAceiteVrpl(domain.getDataAceiteVrpl());
        entity.setParecerTecnicoVrpl(domain.getParecerTecnicoVrpl());
        entity.setObservacoesDiligenciaVrpl(domain.getObservacoesDiligenciaVrpl());
        entity.setNumeroAio(domain.getNumeroAio());
        entity.setDataEmissaoAio(domain.getDataEmissaoAio());
        entity.setStatusAio(nameOrNull(domain.getStatusAio()));
        entity.setCnpjVencedor(domain.getCnpjVencedor());
        entity.setRazaoSocialVencedor(domain.getRazaoSocialVencedor());
        entity.setS3KeyEdital(domain.getS3KeyEdital());
        entity.setS3KeyTermoHomologacao(domain.getS3KeyTermoHomologacao());
        entity.setS3KeyPropostaVencedora(domain.getS3KeyPropostaVencedora());
        entity.setS3KeyAtaSessao(domain.getS3KeyAtaSessao());
        entity.setS3KeyParecerVrpl(domain.getS3KeyParecerVrpl());
        entity.setS3KeyAutorizacaoAio(domain.getS3KeyAutorizacaoAio());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }

    private static String nameOrNull(Enum<?> value) {
        return value != null ? value.name() : null;
    }

    private static <E extends Enum<E>> E enumOrNull(Class<E> type, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Enum.valueOf(type, value);
    }
}
