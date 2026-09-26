package br.com.govflow.core.infrastructure.adapter.out.persistence.repository;

import br.com.govflow.core.infrastructure.adapter.out.persistence.entity.ConvenioJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataConvenioRepository extends JpaRepository<ConvenioJpaEntity, UUID> {

    Optional<ConvenioJpaEntity> findByNumeroSiconv(String numeroSiconv);

    List<ConvenioJpaEntity> findByPrefeituraId(UUID prefeituraId);

    List<ConvenioJpaEntity> findByTenantId(UUID tenantId);
}
