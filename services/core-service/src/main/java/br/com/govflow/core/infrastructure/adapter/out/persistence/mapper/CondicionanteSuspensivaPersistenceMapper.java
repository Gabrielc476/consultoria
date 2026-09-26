package br.com.govflow.core.infrastructure.adapter.out.persistence.mapper;

import br.com.govflow.core.domain.model.convenio.CondicionanteSuspensiva;
import br.com.govflow.core.domain.model.convenio.StatusCondicionanteSuspensiva;
import br.com.govflow.core.domain.model.convenio.TipoCondicionanteSuspensiva;
import br.com.govflow.core.infrastructure.adapter.out.persistence.entity.CondicionanteSuspensivaJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class CondicionanteSuspensivaPersistenceMapper {

    public CondicionanteSuspensiva toDomain(CondicionanteSuspensivaJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        TipoCondicionanteSuspensiva tipo = TipoCondicionanteSuspensiva.valueOf(entity.getTipoCondicionante());
        StatusCondicionanteSuspensiva status = StatusCondicionanteSuspensiva.valueOf(entity.getStatus());

        return new CondicionanteSuspensiva(
                entity.getId(),
                entity.getTenantId(),
                entity.getConvenioId(),
                tipo,
                status,
                entity.getNumeroDocumentoComprobatorio(),
                entity.getDataAprovacao(),
                entity.getDataValidade(),
                entity.getObservacoesAnaliseCaixa(),
                entity.getS3KeyDocumento(),
                entity.getDataLimiteSaneamento(),
                entity.getS3KeyLaudoPendencias(),
                entity.getValorOrcamentoAprovadoCaixa(),
                entity.getPercentualBdiAprovado(),
                entity.getNumeroArtRrt(),
                entity.getOrgaoEmissor(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public CondicionanteSuspensivaJpaEntity toEntity(CondicionanteSuspensiva domain) {
        if (domain == null) {
            return null;
        }

        CondicionanteSuspensivaJpaEntity entity = new CondicionanteSuspensivaJpaEntity();
        entity.setId(domain.getId());
        entity.setTenantId(domain.getTenantId());
        entity.setConvenioId(domain.getConvenioId());
        entity.setTipoCondicionante(domain.getTipoCondicionante().name());
        entity.setStatus(domain.getStatus().name());
        entity.setNumeroDocumentoComprobatorio(domain.getNumeroDocumentoComprobatorio());
        entity.setDataAprovacao(domain.getDataAprovacao());
        entity.setDataValidade(domain.getDataValidade());
        entity.setObservacoesAnaliseCaixa(domain.getObservacoesAnaliseCaixa());
        entity.setS3KeyDocumento(domain.getS3KeyDocumento());
        entity.setDataLimiteSaneamento(domain.getDataLimiteSaneamento());
        entity.setS3KeyLaudoPendencias(domain.getS3KeyLaudoPendencias());
        entity.setValorOrcamentoAprovadoCaixa(domain.getValorOrcamentoAprovadoCaixa());
        entity.setPercentualBdiAprovado(domain.getPercentualBdiAprovado());
        entity.setNumeroArtRrt(domain.getNumeroArtRrt());
        entity.setOrgaoEmissor(domain.getOrgaoEmissor());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
