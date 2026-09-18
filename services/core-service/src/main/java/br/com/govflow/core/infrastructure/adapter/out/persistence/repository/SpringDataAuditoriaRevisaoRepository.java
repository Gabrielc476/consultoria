package br.com.govflow.core.infrastructure.adapter.out.persistence.repository;

import br.com.govflow.core.infrastructure.adapter.out.persistence.entity.AuditoriaRevisaoJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataAuditoriaRevisaoRepository extends JpaRepository<AuditoriaRevisaoJpaEntity, UUID> {

    List<AuditoriaRevisaoJpaEntity> findByDocumentoIdOrderByDataRevisaoDesc(UUID documentoId);
}
