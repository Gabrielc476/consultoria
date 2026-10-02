package br.com.govflow.core.infrastructure.adapter.out.persistence.repository;

import br.com.govflow.core.infrastructure.adapter.out.persistence.entity.ContatoCoreJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataContatoCoreRepository extends JpaRepository<ContatoCoreJpaEntity, UUID> {

    List<ContatoCoreJpaEntity> findByTenantIdOrderByNomeAsc(UUID tenantId);

    Optional<ContatoCoreJpaEntity> findByTenantIdAndPhoneNumber(UUID tenantId, String phoneNumber);

    Optional<ContatoCoreJpaEntity> findByPhoneNumber(String phoneNumber);

    boolean existsByTenantIdAndPhoneNumber(UUID tenantId, String phoneNumber);
}
