package br.com.govflow.transferegov.persistence.repository;

import br.com.govflow.transferegov.persistence.entity.SincronizacaoLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SincronizacaoLogRepository extends JpaRepository<SincronizacaoLogEntity, UUID> {

    Optional<SincronizacaoLogEntity> findTopByOrderByDataInicioDesc();

    Optional<SincronizacaoLogEntity> findTopByStatusOrderByDataInicioDesc(String status);
}
