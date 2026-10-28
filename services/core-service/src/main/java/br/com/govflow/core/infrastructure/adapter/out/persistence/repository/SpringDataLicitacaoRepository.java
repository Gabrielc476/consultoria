package br.com.govflow.core.infrastructure.adapter.out.persistence.repository;

import br.com.govflow.core.infrastructure.adapter.out.persistence.entity.LicitacaoJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataLicitacaoRepository extends JpaRepository<LicitacaoJpaEntity, UUID> {

    List<LicitacaoJpaEntity> findByConvenioIdOrderByCreatedAtAsc(UUID convenioId);

    boolean existsByConvenioIdAndNumeroLicitacao(UUID convenioId, String numeroLicitacao);
}
