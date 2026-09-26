package br.com.govflow.core.infrastructure.adapter.out.persistence.repository;

import br.com.govflow.core.infrastructure.adapter.out.persistence.entity.CondicionanteSuspensivaJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataCondicionanteSuspensivaRepository extends JpaRepository<CondicionanteSuspensivaJpaEntity, UUID> {

    List<CondicionanteSuspensivaJpaEntity> findByConvenioId(UUID convenioId);

    Optional<CondicionanteSuspensivaJpaEntity> findByConvenioIdAndTipoCondicionante(UUID convenioId, String tipoCondicionante);

    List<CondicionanteSuspensivaJpaEntity> findByTenantId(UUID tenantId);
}
