package br.com.govflow.core.infrastructure.adapter.out.persistence.repository;

import br.com.govflow.core.infrastructure.adapter.out.persistence.entity.DocumentoJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataDocumentoRepository extends JpaRepository<DocumentoJpaEntity, UUID> {

    Optional<DocumentoJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<DocumentoJpaEntity> findByTenantId(UUID tenantId, Pageable pageable);

    Page<DocumentoJpaEntity> findByTenantIdAndStatus(UUID tenantId, String status, Pageable pageable);

    Page<DocumentoJpaEntity> findByStatus(String status, Pageable pageable);

    long countByStatus(String status);

    long countByTenantId(UUID tenantId);

    long countByTenantIdAndStatus(UUID tenantId, String status);

    List<DocumentoJpaEntity> findByTenantIdAndConvenioId(UUID tenantId, UUID convenioId);

    List<DocumentoJpaEntity> findByTenantIdAndConvenioIdAndFaseCicloVida(UUID tenantId, UUID convenioId, String faseCicloVida);

    List<DocumentoJpaEntity> findByTenantIdAndConvenioIdAndPastaVirtual(UUID tenantId, UUID convenioId, String pastaVirtual);

    List<DocumentoJpaEntity> findByConvenioId(UUID convenioId);

    List<DocumentoJpaEntity> findByConvenioIdAndFaseCicloVida(UUID convenioId, String faseCicloVida);
}
