package br.com.govflow.transferegov.persistence.repository;

import br.com.govflow.transferegov.domain.compliance.SeveridadeInconformidade;
import br.com.govflow.transferegov.persistence.entity.EmendaEspecialInconformidadeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface EmendaEspecialInconformidadeRepository extends JpaRepository<EmendaEspecialInconformidadeEntity, UUID> {

    List<EmendaEspecialInconformidadeEntity> findByResolvidoFalse();

    List<EmendaEspecialInconformidadeEntity> findByPlanoAcaoIdAndResolvidoFalse(UUID planoAcaoId);

    long countBySeveridadeAndResolvidoFalse(SeveridadeInconformidade severidade);

    @Query("SELECT i FROM EmendaEspecialInconformidadeEntity i WHERE i.resolvido = false AND i.severidade = :severidade ORDER BY i.dataDeteccao DESC")
    List<EmendaEspecialInconformidadeEntity> findTopBySeveridade(@Param("severidade") SeveridadeInconformidade severidade);
}
