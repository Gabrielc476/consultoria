package br.com.govflow.core.infrastructure.adapter.out.persistence.repository;

import br.com.govflow.core.infrastructure.adapter.out.persistence.entity.TriagemInboxJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataTriagemInboxRepository extends JpaRepository<TriagemInboxJpaEntity, UUID> {

    List<TriagemInboxJpaEntity> findByTenantIdAndStatusOrderByCreatedAtDesc(UUID tenantId, String status);

    List<TriagemInboxJpaEntity> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    Optional<TriagemInboxJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    long countByTenantIdAndStatus(UUID tenantId, String status);

    boolean existsByDocumentoId(UUID documentoId);
}
