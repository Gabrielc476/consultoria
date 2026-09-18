package br.com.govflow.core.infrastructure.adapter.out.persistence;

import br.com.govflow.core.application.port.out.DocumentoRepositoryPort;
import br.com.govflow.core.domain.model.Documento;
import br.com.govflow.core.domain.model.StatusDocumento;
import br.com.govflow.core.infrastructure.adapter.out.persistence.entity.DocumentoJpaEntity;
import br.com.govflow.core.infrastructure.adapter.out.persistence.mapper.DocumentoPersistenceMapper;
import br.com.govflow.core.infrastructure.adapter.out.persistence.repository.SpringDataDocumentoRepository;
import br.com.govflow.core.infrastructure.interceptor.TenantContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class DocumentoRepositoryAdapter implements DocumentoRepositoryPort {

    private final SpringDataDocumentoRepository repository;
    private final DocumentoPersistenceMapper mapper;

    public DocumentoRepositoryAdapter(SpringDataDocumentoRepository repository,
                                      DocumentoPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Documento salvar(Documento documento) {
        DocumentoJpaEntity entity = mapper.toEntity(documento);
        DocumentoJpaEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Documento> buscarPorId(UUID id) {
        UUID tenantId = TenantContext.getCurrentTenant();
        if (tenantId != null) {
            return repository.findByIdAndTenantId(id, tenantId).map(mapper::toDomain);
        }
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Documento> listar(int page, int size, StatusDocumento status) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        UUID tenantId = TenantContext.getCurrentTenant();

        if (tenantId != null) {
            if (status != null) {
                return repository.findByTenantIdAndStatus(tenantId, status.name(), pageRequest)
                        .map(mapper::toDomain)
                        .getContent();
            }
            return repository.findByTenantId(tenantId, pageRequest)
                    .map(mapper::toDomain)
                    .getContent();
        }

        if (status != null) {
            return repository.findByStatus(status.name(), pageRequest)
                    .map(mapper::toDomain)
                    .getContent();
        }
        return repository.findAll(pageRequest)
                .map(mapper::toDomain)
                .getContent();
    }

    @Override
    public long contarPorStatus(StatusDocumento status) {
        UUID tenantId = TenantContext.getCurrentTenant();
        if (tenantId != null) {
            if (status != null) {
                return repository.countByTenantIdAndStatus(tenantId, status.name());
            }
            return repository.countByTenantId(tenantId);
        }

        if (status != null) {
            return repository.countByStatus(status.name());
        }
        return repository.count();
    }
}
