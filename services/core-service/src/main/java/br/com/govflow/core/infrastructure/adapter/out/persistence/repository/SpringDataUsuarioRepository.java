package br.com.govflow.core.infrastructure.adapter.out.persistence.repository;

import br.com.govflow.core.infrastructure.adapter.out.persistence.entity.UsuarioJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataUsuarioRepository extends JpaRepository<UsuarioJpaEntity, UUID> {

    Optional<UsuarioJpaEntity> findByEmail(String email);

    Optional<UsuarioJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<UsuarioJpaEntity> findByEmailAndTenantId(String email, UUID tenantId);

    Optional<UsuarioJpaEntity> findByTelefoneCelular(String telefoneCelular);

    List<UsuarioJpaEntity> findByTenantId(UUID tenantId);

    List<UsuarioJpaEntity> findByTenantIdAndRole(UUID tenantId, String role);

    boolean existsByEmail(String email);
}
