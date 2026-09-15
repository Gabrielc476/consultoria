package br.com.govflow.core.infrastructure.adapter.out.persistence;

import br.com.govflow.core.application.port.out.ConsultoriaRepositoryPort;
import br.com.govflow.core.domain.model.Cnpj;
import br.com.govflow.core.domain.model.Consultoria;
import br.com.govflow.core.infrastructure.adapter.out.persistence.entity.ConsultoriaJpaEntity;
import br.com.govflow.core.infrastructure.adapter.out.persistence.mapper.ConsultoriaPersistenceMapper;
import br.com.govflow.core.infrastructure.adapter.out.persistence.repository.SpringDataConsultoriaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class ConsultoriaRepositoryAdapter implements ConsultoriaRepositoryPort {

    private final SpringDataConsultoriaRepository repository;
    private final ConsultoriaPersistenceMapper mapper;

    public ConsultoriaRepositoryAdapter(SpringDataConsultoriaRepository repository,
                                       ConsultoriaPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Consultoria salvar(Consultoria consultoria) {
        ConsultoriaJpaEntity entity = mapper.toEntity(consultoria);
        ConsultoriaJpaEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Consultoria> buscarPorId(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Consultoria> buscarPorCnpj(Cnpj cnpj) {
        return repository.findByCnpj(cnpj.getFormatted())
                .or(() -> repository.findByCnpj(cnpj.getValue()))
                .map(mapper::toDomain);
    }

    @Override
    public boolean existePorCnpj(Cnpj cnpj) {
        return repository.existsByCnpj(cnpj.getFormatted())
                || repository.existsByCnpj(cnpj.getValue());
    }
}
