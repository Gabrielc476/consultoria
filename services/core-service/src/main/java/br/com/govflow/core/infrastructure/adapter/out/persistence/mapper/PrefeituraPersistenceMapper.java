package br.com.govflow.core.infrastructure.adapter.out.persistence.mapper;

import br.com.govflow.core.domain.model.Cnpj;
import br.com.govflow.core.domain.model.CodigoIbge;
import br.com.govflow.core.domain.model.Cpf;
import br.com.govflow.core.domain.model.PorteMunicipio;
import br.com.govflow.core.domain.model.Prefeitura;
import br.com.govflow.core.domain.model.StatusCauc;
import br.com.govflow.core.domain.model.Uf;
import br.com.govflow.core.infrastructure.adapter.out.persistence.entity.PrefeituraJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class PrefeituraPersistenceMapper {

    public Prefeitura toDomain(PrefeituraJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        Cnpj cnpj = new Cnpj(entity.getCnpj());
        Uf uf = Uf.fromString(entity.getUf())
                .orElseThrow(() -> new IllegalStateException("UF inválida persistida: " + entity.getUf()));
        CodigoIbge codigoIbge = new CodigoIbge(entity.getCodigoIbge());
        PorteMunicipio porte = PorteMunicipio.valueOf(entity.getPorteMunicipio());
        StatusCauc statusCauc = StatusCauc.valueOf(entity.getStatusCauc());
        Cpf cpfPrefeito = (entity.getCpfPrefeito() != null && !entity.getCpfPrefeito().isBlank())
                ? new Cpf(entity.getCpfPrefeito())
                : null;

        return new Prefeitura(
                entity.getId(),
                entity.getTenantId(),
                cnpj,
                entity.getRazaoSocial(),
                entity.getNomeMunicipio(),
                uf,
                codigoIbge,
                porte,
                entity.getNomePrefeito(),
                cpfPrefeito,
                entity.getInicioMandato(),
                entity.getFimMandato(),
                statusCauc,
                entity.isAtivo(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public PrefeituraJpaEntity toEntity(Prefeitura domain) {
        if (domain == null) {
            return null;
        }

        PrefeituraJpaEntity entity = new PrefeituraJpaEntity();
        entity.setId(domain.getId());
        entity.setTenantId(domain.getTenantId());
        entity.setCnpj(domain.getCnpj().getFormatted());
        entity.setRazaoSocial(domain.getRazaoSocial());
        entity.setNomeMunicipio(domain.getNomeMunicipio());
        entity.setUf(domain.getUf().name());
        entity.setCodigoIbge(domain.getCodigoIbge().getValue());
        entity.setPorteMunicipio(domain.getPorteMunicipio().name());
        entity.setNomePrefeito(domain.getNomePrefeito());
        entity.setCpfPrefeito(domain.getCpfPrefeito() != null ? domain.getCpfPrefeito().getFormatted() : null);
        entity.setInicioMandato(domain.getInicioMandato());
        entity.setFimMandato(domain.getFimMandato());
        entity.setStatusCauc(domain.getStatusCauc().name());
        entity.setAtivo(domain.isAtivo());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());

        return entity;
    }
}
