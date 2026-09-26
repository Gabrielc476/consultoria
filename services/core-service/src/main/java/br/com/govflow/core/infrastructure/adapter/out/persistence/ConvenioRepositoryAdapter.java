package br.com.govflow.core.infrastructure.adapter.out.persistence;

import br.com.govflow.core.application.port.out.ConvenioRepositoryPort;
import br.com.govflow.core.domain.model.convenio.Convenio;
import br.com.govflow.core.infrastructure.adapter.out.persistence.entity.ConvenioJpaEntity;
import br.com.govflow.core.infrastructure.adapter.out.persistence.mapper.ConvenioPersistenceMapper;
import br.com.govflow.core.infrastructure.adapter.out.persistence.repository.SpringDataConvenioRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ConvenioRepositoryAdapter implements ConvenioRepositoryPort {

    private final SpringDataConvenioRepository repository;
    private final ConvenioPersistenceMapper mapper;

    public ConvenioRepositoryAdapter(SpringDataConvenioRepository repository,
                                   ConvenioPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Convenio> buscarPorId(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Convenio> buscarPorNumeroSiconv(String numeroSiconv) {
        return repository.findByNumeroSiconv(numeroSiconv).map(mapper::toDomain);
    }

    @Override
    public List<Convenio> listarPorPrefeitura(UUID prefeituraId) {
        return repository.findByPrefeituraId(prefeituraId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Convenio salvar(Convenio convenio) {
        ConvenioJpaEntity entity = mapper.toEntity(convenio);
        ConvenioJpaEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }
}
