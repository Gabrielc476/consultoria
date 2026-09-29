package br.com.govflow.core.infrastructure.adapter.out.persistence.repository;

import br.com.govflow.core.infrastructure.adapter.out.persistence.entity.UsuarioPrefeituraId;
import br.com.govflow.core.infrastructure.adapter.out.persistence.entity.UsuarioPrefeituraJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataUsuarioPrefeituraRepository extends JpaRepository<UsuarioPrefeituraJpaEntity, UsuarioPrefeituraId> {

    List<UsuarioPrefeituraJpaEntity> findByUsuarioId(UUID usuarioId);

    List<UsuarioPrefeituraJpaEntity> findByPrefeituraId(UUID prefeituraId);

    void deleteByUsuarioId(UUID usuarioId);

    boolean existsByUsuarioIdAndPrefeituraId(UUID usuarioId, UUID prefeituraId);
}
