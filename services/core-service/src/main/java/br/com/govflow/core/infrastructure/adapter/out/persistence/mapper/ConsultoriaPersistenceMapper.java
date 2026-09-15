package br.com.govflow.core.infrastructure.adapter.out.persistence.mapper;

import br.com.govflow.core.domain.model.Cnpj;
import br.com.govflow.core.domain.model.Consultoria;
import br.com.govflow.core.domain.model.PlanoConsultoria;
import br.com.govflow.core.domain.model.StatusConsultoria;
import br.com.govflow.core.infrastructure.adapter.out.persistence.entity.ConsultoriaJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class ConsultoriaPersistenceMapper {

    public Consultoria toDomain(ConsultoriaJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        Cnpj cnpj = new Cnpj(entity.getCnpj());
        PlanoConsultoria plano = PlanoConsultoria.valueOf(entity.getPlano());
        StatusConsultoria status = StatusConsultoria.valueOf(entity.getStatus());

        return new Consultoria(
                entity.getId(),
                cnpj,
                entity.getRazaoSocial(),
                entity.getNomeFantasia(),
                entity.getEmailContato(),
                entity.getTelefoneContato(),
                plano,
                status,
                entity.getLimitePrefeituras(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public ConsultoriaJpaEntity toEntity(Consultoria domain) {
        if (domain == null) {
            return null;
        }

        ConsultoriaJpaEntity entity = new ConsultoriaJpaEntity();
        entity.setId(domain.getId());
        entity.setCnpj(domain.getCnpj().getFormatted());
        entity.setRazaoSocial(domain.getRazaoSocial());
        entity.setNomeFantasia(domain.getNomeFantasia());
        entity.setEmailContato(domain.getEmailContato());
        entity.setTelefoneContato(domain.getTelefoneContato());
        entity.setPlano(domain.getPlano().name());
        entity.setStatus(domain.getStatus().name());
        entity.setLimitePrefeituras(domain.getLimitePrefeituras());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());

        return entity;
    }
}
