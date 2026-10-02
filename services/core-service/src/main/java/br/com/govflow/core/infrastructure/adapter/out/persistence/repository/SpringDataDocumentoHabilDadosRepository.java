package br.com.govflow.core.infrastructure.adapter.out.persistence.repository;

import br.com.govflow.core.infrastructure.adapter.out.persistence.entity.DocumentoHabilDadosJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataDocumentoHabilDadosRepository extends JpaRepository<DocumentoHabilDadosJpaEntity, UUID> {

    Optional<DocumentoHabilDadosJpaEntity> findByDocumentoId(UUID documentoId);
}
