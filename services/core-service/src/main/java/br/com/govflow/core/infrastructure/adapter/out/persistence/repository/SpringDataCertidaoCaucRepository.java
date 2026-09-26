package br.com.govflow.core.infrastructure.adapter.out.persistence.repository;

import br.com.govflow.core.infrastructure.adapter.out.persistence.entity.CertidaoCaucJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataCertidaoCaucRepository extends JpaRepository<CertidaoCaucJpaEntity, UUID> {

    List<CertidaoCaucJpaEntity> findByPrefeituraId(UUID prefeituraId);

    Optional<CertidaoCaucJpaEntity> findByPrefeituraIdAndTipoExigencia(UUID prefeituraId, String tipoExigencia);

    long countByPrefeituraIdAndSituacao(UUID prefeituraId, String situacao);

    List<CertidaoCaucJpaEntity> findByTenantId(UUID tenantId);
}
