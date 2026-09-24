package br.com.govflow.transferegov.persistence.repository;

import br.com.govflow.transferegov.persistence.entity.SincronizacaoAnomaliaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SincronizacaoAnomaliaRepository extends JpaRepository<SincronizacaoAnomaliaEntity, UUID> {

    List<SincronizacaoAnomaliaEntity> findByLogId(UUID logId);

    Page<SincronizacaoAnomaliaEntity> findByLogId(UUID logId, Pageable pageable);

    long countByLogId(UUID logId);
}
