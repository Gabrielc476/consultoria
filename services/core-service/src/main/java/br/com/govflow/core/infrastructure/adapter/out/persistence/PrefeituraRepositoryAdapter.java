package br.com.govflow.core.infrastructure.adapter.out.persistence;

import br.com.govflow.core.application.port.out.PrefeituraRepositoryPort;
import br.com.govflow.core.domain.model.Cnpj;
import br.com.govflow.core.domain.model.Prefeitura;
import br.com.govflow.core.infrastructure.adapter.out.persistence.entity.PrefeituraJpaEntity;
import br.com.govflow.core.infrastructure.adapter.out.persistence.mapper.PrefeituraPersistenceMapper;
import br.com.govflow.core.infrastructure.adapter.out.persistence.repository.SpringDataPrefeituraRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class PrefeituraRepositoryAdapter implements PrefeituraRepositoryPort {

    private final SpringDataPrefeituraRepository repository;
    private final PrefeituraPersistenceMapper mapper;

    public PrefeituraRepositoryAdapter(SpringDataPrefeituraRepository repository,
                                       PrefeituraPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Prefeitura salvar(Prefeitura prefeitura) {
        PrefeituraJpaEntity entity = mapper.toEntity(prefeitura);
        PrefeituraJpaEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Prefeitura> buscarPorId(UUID id) {
        UUID tenantId = br.com.govflow.core.infrastructure.interceptor.TenantContext.getCurrentTenant();
        if (tenantId != null) {
            return repository.findByIdAndTenantId(id, tenantId).map(mapper::toDomain);
        }
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Prefeitura> listar(int page, int size, Boolean ativo) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("nomeMunicipio").ascending());
        if (ativo != null) {
            return repository.findByAtivo(ativo, pageRequest)
                    .map(mapper::toDomain)
                    .getContent();
        }
        return repository.findAll(pageRequest)
                .map(mapper::toDomain)
                .getContent();
    }

    @Override
    public long contar(Boolean ativo) {
        if (ativo != null) {
            return repository.countByAtivo(ativo);
        }
        return repository.count();
    }

    @Override
    public boolean existePorCnpjETenantId(Cnpj cnpj, UUID tenantId) {
        return repository.existsByCnpjAndTenantId(cnpj.getFormatted(), tenantId)
                || repository.existsByCnpjAndTenantId(cnpj.getValue(), tenantId);
    }

    @Override
    public long contarPorTenantId(UUID tenantId) {
        return repository.countByTenantId(tenantId);
    }
}
