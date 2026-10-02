package br.com.govflow.core.infrastructure.adapter.out.persistence;

import br.com.govflow.core.application.port.out.DocumentoRepositoryPort;
import br.com.govflow.core.domain.model.Documento;
import br.com.govflow.core.domain.model.StatusDocumento;
import br.com.govflow.core.domain.model.documento.DocumentoAuditoria;
import br.com.govflow.core.domain.model.documento.DocumentoHabilDados;
import br.com.govflow.core.domain.model.documento.FaseCicloVida;
import br.com.govflow.core.infrastructure.adapter.out.persistence.entity.DocumentoAuditoriaJpaEntity;
import br.com.govflow.core.infrastructure.adapter.out.persistence.entity.DocumentoHabilDadosJpaEntity;
import br.com.govflow.core.infrastructure.adapter.out.persistence.entity.DocumentoJpaEntity;
import br.com.govflow.core.infrastructure.adapter.out.persistence.mapper.DocumentoPersistenceMapper;
import br.com.govflow.core.infrastructure.adapter.out.persistence.repository.SpringDataDocumentoAuditoriaRepository;
import br.com.govflow.core.infrastructure.adapter.out.persistence.repository.SpringDataDocumentoHabilDadosRepository;
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
    private final SpringDataDocumentoHabilDadosRepository dadosHabeisRepository;
    private final SpringDataDocumentoAuditoriaRepository auditoriaRepository;
    private final DocumentoPersistenceMapper mapper;

    public DocumentoRepositoryAdapter(SpringDataDocumentoRepository repository,
                                      SpringDataDocumentoHabilDadosRepository dadosHabeisRepository,
                                      SpringDataDocumentoAuditoriaRepository auditoriaRepository,
                                      DocumentoPersistenceMapper mapper) {
        this.repository = repository;
        this.dadosHabeisRepository = dadosHabeisRepository;
        this.auditoriaRepository = auditoriaRepository;
        this.mapper = mapper;
    }

    @Override
    public Documento salvar(Documento documento) {
        DocumentoJpaEntity entity = mapper.toEntity(documento);
        DocumentoJpaEntity saved = repository.save(entity);

        if (documento.getDadosHabeis() != null) {
            salvarDadosHabeis(documento.getDadosHabeis());
        }

        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Documento> buscarPorId(UUID id) {
        UUID tenantId = TenantContext.getCurrentTenant();
        Optional<DocumentoJpaEntity> entityOpt;
        if (tenantId != null) {
            entityOpt = repository.findByIdAndTenantId(id, tenantId);
        } else {
            entityOpt = repository.findById(id);
        }

        return entityOpt.map(entity -> {
            Documento doc = mapper.toDomain(entity);
            dadosHabeisRepository.findByDocumentoId(id)
                    .ifPresent(h -> doc.vincularDadosHabeis(mapper.toDomain(h)));
            return doc;
        });
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

    @Override
    public List<Documento> listarPorConvenioId(UUID convenioId) {
        UUID tenantId = TenantContext.getCurrentTenant();
        List<DocumentoJpaEntity> entities;
        if (tenantId != null) {
            entities = repository.findByTenantIdAndConvenioId(tenantId, convenioId);
        } else {
            entities = repository.findByConvenioId(convenioId);
        }
        return entities.stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Documento> listarPorConvenioIdEFase(UUID convenioId, FaseCicloVida fase) {
        UUID tenantId = TenantContext.getCurrentTenant();
        List<DocumentoJpaEntity> entities;
        String faseStr = fase != null ? fase.name() : FaseCicloVida.FASE_05_EXECUCAO_FINANCEIRA.name();
        if (tenantId != null) {
            entities = repository.findByTenantIdAndConvenioIdAndFaseCicloVida(tenantId, convenioId, faseStr);
        } else {
            entities = repository.findByConvenioIdAndFaseCicloVida(convenioId, faseStr);
        }
        return entities.stream().map(mapper::toDomain).toList();
    }

    @Override
    public void excluir(UUID id) {
        repository.deleteById(id);
    }

    @Override
    public void salvarAuditoria(DocumentoAuditoria auditoria) {
        DocumentoAuditoriaJpaEntity entity = mapper.toEntity(auditoria);
        auditoriaRepository.save(entity);
    }

    @Override
    public List<DocumentoAuditoria> listarAuditorias(UUID documentoId) {
        UUID tenantId = TenantContext.getCurrentTenant();
        List<DocumentoAuditoriaJpaEntity> entities;
        if (tenantId != null) {
            entities = auditoriaRepository.findByTenantIdAndDocumentoIdOrderByRealizadoEmDesc(tenantId, documentoId);
        } else {
            entities = auditoriaRepository.findByDocumentoIdOrderByRealizadoEmDesc(documentoId);
        }
        return entities.stream().map(mapper::toDomain).toList();
    }

    @Override
    public void salvarDadosHabeis(DocumentoHabilDados dadosHabeis) {
        DocumentoHabilDadosJpaEntity entity = mapper.toEntity(dadosHabeis);
        dadosHabeisRepository.save(entity);
    }

    @Override
    public Optional<DocumentoHabilDados> buscarDadosHabeis(UUID documentoId) {
        return dadosHabeisRepository.findByDocumentoId(documentoId)
                .map(mapper::toDomain);
    }
}
