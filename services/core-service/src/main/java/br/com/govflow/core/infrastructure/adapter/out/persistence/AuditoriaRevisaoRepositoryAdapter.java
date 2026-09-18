package br.com.govflow.core.infrastructure.adapter.out.persistence;

import br.com.govflow.core.application.port.out.AuditoriaRevisaoRepositoryPort;
import br.com.govflow.core.domain.model.AuditoriaRevisao;
import br.com.govflow.core.infrastructure.adapter.out.persistence.entity.AuditoriaRevisaoJpaEntity;
import br.com.govflow.core.infrastructure.adapter.out.persistence.mapper.AuditoriaRevisaoPersistenceMapper;
import br.com.govflow.core.infrastructure.adapter.out.persistence.repository.SpringDataAuditoriaRevisaoRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class AuditoriaRevisaoRepositoryAdapter implements AuditoriaRevisaoRepositoryPort {

    private final SpringDataAuditoriaRevisaoRepository repository;
    private final AuditoriaRevisaoPersistenceMapper mapper;

    public AuditoriaRevisaoRepositoryAdapter(SpringDataAuditoriaRevisaoRepository repository,
                                            AuditoriaRevisaoPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public AuditoriaRevisao salvar(AuditoriaRevisao auditoria) {
        AuditoriaRevisaoJpaEntity entity = mapper.toEntity(auditoria);
        AuditoriaRevisaoJpaEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<AuditoriaRevisao> listarPorDocumentoId(UUID documentoId) {
        return repository.findByDocumentoIdOrderByDataRevisaoDesc(documentoId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
