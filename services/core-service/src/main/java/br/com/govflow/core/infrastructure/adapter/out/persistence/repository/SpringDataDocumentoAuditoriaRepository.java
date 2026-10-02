package br.com.govflow.core.infrastructure.adapter.out.persistence.repository;

import br.com.govflow.core.infrastructure.adapter.out.persistence.entity.DocumentoAuditoriaJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataDocumentoAuditoriaRepository extends JpaRepository<DocumentoAuditoriaJpaEntity, UUID> {

    List<DocumentoAuditoriaJpaEntity> findByDocumentoIdOrderByRealizadoEmDesc(UUID documentoId);

    List<DocumentoAuditoriaJpaEntity> findByTenantIdAndDocumentoIdOrderByRealizadoEmDesc(UUID tenantId, UUID documentoId);
}
