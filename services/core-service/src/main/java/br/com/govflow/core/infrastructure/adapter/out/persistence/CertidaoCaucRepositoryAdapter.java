package br.com.govflow.core.infrastructure.adapter.out.persistence;

import br.com.govflow.core.application.port.out.CertidaoCaucRepositoryPort;
import br.com.govflow.core.domain.model.CertidaoCauc;
import br.com.govflow.core.domain.model.StatusCertidao;
import br.com.govflow.core.domain.model.TipoExigenciaCauc;
import br.com.govflow.core.infrastructure.adapter.out.persistence.entity.CertidaoCaucJpaEntity;
import br.com.govflow.core.infrastructure.adapter.out.persistence.mapper.CertidaoCaucPersistenceMapper;
import br.com.govflow.core.infrastructure.adapter.out.persistence.repository.SpringDataCertidaoCaucRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class CertidaoCaucRepositoryAdapter implements CertidaoCaucRepositoryPort {

    private final SpringDataCertidaoCaucRepository repository;
    private final CertidaoCaucPersistenceMapper mapper;

    public CertidaoCaucRepositoryAdapter(SpringDataCertidaoCaucRepository repository,
                                         CertidaoCaucPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public CertidaoCauc salvar(CertidaoCauc certidao) {
        CertidaoCaucJpaEntity entity = mapper.toEntity(certidao);
        CertidaoCaucJpaEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<CertidaoCauc> salvarTodas(List<CertidaoCauc> certidoes) {
        List<CertidaoCaucJpaEntity> entities = certidoes.stream().map(mapper::toEntity).toList();
        return repository.saveAll(entities).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<CertidaoCauc> buscarPorId(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<CertidaoCauc> buscarPorPrefeituraIdETipoExigencia(UUID prefeituraId, TipoExigenciaCauc tipoExigencia) {
        return repository.findByPrefeituraIdAndTipoExigencia(prefeituraId, tipoExigencia.name())
                .map(mapper::toDomain);
    }

    @Override
    public List<CertidaoCauc> listarPorPrefeituraId(UUID prefeituraId) {
        return repository.findByPrefeituraId(prefeituraId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<CertidaoCauc> listarTodasPorTenant(UUID tenantId) {
        return repository.findByTenantId(tenantId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<CertidaoCauc> listarTodas() {
        return repository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public long contarPorPrefeituraIdESituacao(UUID prefeituraId, StatusCertidao situacao) {
        return repository.countByPrefeituraIdAndSituacao(prefeituraId, situacao.name());
    }
}
