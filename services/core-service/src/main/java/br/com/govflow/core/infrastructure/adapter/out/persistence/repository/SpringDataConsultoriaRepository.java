package br.com.govflow.core.infrastructure.adapter.out.persistence.repository;

import br.com.govflow.core.infrastructure.adapter.out.persistence.entity.ConsultoriaJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataConsultoriaRepository extends JpaRepository<ConsultoriaJpaEntity, UUID> {

    Optional<ConsultoriaJpaEntity> findByCnpj(String cnpj);

    boolean existsByCnpj(String cnpj);
}
