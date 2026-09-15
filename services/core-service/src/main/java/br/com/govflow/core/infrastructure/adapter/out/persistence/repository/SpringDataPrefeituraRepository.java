package br.com.govflow.core.infrastructure.adapter.out.persistence.repository;

import br.com.govflow.core.infrastructure.adapter.out.persistence.entity.PrefeituraJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataPrefeituraRepository extends JpaRepository<PrefeituraJpaEntity, UUID> {

    Optional<PrefeituraJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<PrefeituraJpaEntity> findByCnpj(String cnpj);

    boolean existsByCnpj(String cnpj);

    boolean existsByCnpjAndTenantId(String cnpj, UUID tenantId);

    Page<PrefeituraJpaEntity> findByAtivo(boolean ativo, Pageable pageable);

    long countByAtivo(boolean ativo);

    long countByTenantId(UUID tenantId);
}
