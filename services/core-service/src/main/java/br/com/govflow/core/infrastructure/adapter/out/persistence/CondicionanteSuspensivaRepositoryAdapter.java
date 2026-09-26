package br.com.govflow.core.infrastructure.adapter.out.persistence;

import br.com.govflow.core.application.port.out.CondicionanteSuspensivaRepositoryPort;
import br.com.govflow.core.domain.model.convenio.CondicionanteSuspensiva;
import br.com.govflow.core.domain.model.convenio.TipoCondicionanteSuspensiva;
import br.com.govflow.core.infrastructure.adapter.out.persistence.entity.CondicionanteSuspensivaJpaEntity;
import br.com.govflow.core.infrastructure.adapter.out.persistence.mapper.CondicionanteSuspensivaPersistenceMapper;
import br.com.govflow.core.infrastructure.adapter.out.persistence.repository.SpringDataCondicionanteSuspensivaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class CondicionanteSuspensivaRepositoryAdapter implements CondicionanteSuspensivaRepositoryPort {

    private final SpringDataCondicionanteSuspensivaRepository repository;
    private final CondicionanteSuspensivaPersistenceMapper mapper;

    public CondicionanteSuspensivaRepositoryAdapter(SpringDataCondicionanteSuspensivaRepository repository,
                                                   CondicionanteSuspensivaPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<CondicionanteSuspensiva> buscarPorConvenioId(UUID convenioId) {
        return repository.findByConvenioId(convenioId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<CondicionanteSuspensiva> buscarPorConvenioETipo(UUID convenioId, TipoCondicionanteSuspensiva tipo) {
        return repository.findByConvenioIdAndTipoCondicionante(convenioId, tipo.name())
                .map(mapper::toDomain);
    }

    @Override
    public Optional<CondicionanteSuspensiva> buscarPorId(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public CondicionanteSuspensiva salvar(CondicionanteSuspensiva condicionante) {
        CondicionanteSuspensivaJpaEntity entity = mapper.toEntity(condicionante);
        CondicionanteSuspensivaJpaEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<CondicionanteSuspensiva> salvarTodas(List<CondicionanteSuspensiva> condicionantes) {
        List<CondicionanteSuspensivaJpaEntity> entities = condicionantes.stream()
                .map(mapper::toEntity)
                .toList();
        return repository.saveAll(entities).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
