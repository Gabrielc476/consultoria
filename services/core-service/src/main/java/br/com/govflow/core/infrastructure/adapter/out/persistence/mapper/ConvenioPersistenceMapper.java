package br.com.govflow.core.infrastructure.adapter.out.persistence.mapper;

import br.com.govflow.core.domain.model.convenio.Convenio;
import br.com.govflow.core.infrastructure.adapter.out.persistence.entity.ConvenioJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class ConvenioPersistenceMapper {

    public Convenio toDomain(ConvenioJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return new Convenio(
                entity.getId(),
                entity.getTenantId(),
                entity.getPrefeituraId(),
                entity.getNumeroSiconv(),
                entity.getNumeroProcesso(),
                entity.getOrgaoConcedente(),
                entity.getObjeto(),
                entity.getValorGlobal(),
                entity.getValorRepasse(),
                entity.getValorContrapartida(),
                entity.getSituacao(),
                entity.isPossuiClausulaSuspensiva(),
                entity.getPrazoClausulaSuspensiva(),
                entity.getDataInicioVigencia(),
                entity.getDataFimVigencia(),
                entity.isProrrogacaoSolicitada(),
                entity.getNovoPrazoProrrogado(),
                entity.getS3KeyTermoRetiradaSuspensiva(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public ConvenioJpaEntity toEntity(Convenio domain) {
        if (domain == null) {
            return null;
        }

        ConvenioJpaEntity entity = new ConvenioJpaEntity();
        entity.setId(domain.getId());
        entity.setTenantId(domain.getTenantId());
        entity.setPrefeituraId(domain.getPrefeituraId());
        entity.setNumeroSiconv(domain.getNumeroSiconv());
        entity.setNumeroProcesso(domain.getNumeroProcesso());
        entity.setOrgaoConcedente(domain.getOrgaoConcedente());
        entity.setObjeto(domain.getObjeto());
        entity.setValorGlobal(domain.getValorGlobal());
        entity.setValorRepasse(domain.getValorRepasse());
        entity.setValorContrapartida(domain.getValorContrapartida());
        entity.setSituacao(domain.getSituacao());
        entity.setPossuiClausulaSuspensiva(domain.isPossuiClausulaSuspensiva());
        entity.setPrazoClausulaSuspensiva(domain.getPrazoClausulaSuspensiva());
        entity.setDataInicioVigencia(domain.getDataInicioVigencia());
        entity.setDataFimVigencia(domain.getDataFimVigencia());
        entity.setProrrogacaoSolicitada(domain.isProrrogacaoSolicitada());
        entity.setNovoPrazoProrrogado(domain.getNovoPrazoProrrogado());
        entity.setS3KeyTermoRetiradaSuspensiva(domain.getS3KeyTermoRetiradaSuspensiva());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
