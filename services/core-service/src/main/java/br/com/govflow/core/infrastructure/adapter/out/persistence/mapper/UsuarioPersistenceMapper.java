package br.com.govflow.core.infrastructure.adapter.out.persistence.mapper;

import br.com.govflow.core.domain.model.usuario.RoleUsuario;
import br.com.govflow.core.domain.model.usuario.Usuario;
import br.com.govflow.core.infrastructure.adapter.out.persistence.entity.UsuarioJpaEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

@Component
public class UsuarioPersistenceMapper {

    public Usuario toDomain(UsuarioJpaEntity entity) {
        return toDomain(entity, Collections.emptySet());
    }

    public Usuario toDomain(UsuarioJpaEntity entity, Set<UUID> prefeiturasAtribuidasIds) {
        if (entity == null) {
            return null;
        }

        RoleUsuario role = RoleUsuario.valueOf(entity.getRole());

        return new Usuario(
                entity.getId(),
                entity.getTenantId(),
                entity.getNome(),
                entity.getEmail(),
                entity.getSenhaHash(),
                entity.getTelefoneCelular(),
                role,
                entity.isAtivo(),
                prefeiturasAtribuidasIds,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public UsuarioJpaEntity toEntity(Usuario domain) {
        if (domain == null) {
            return null;
        }

        UsuarioJpaEntity entity = new UsuarioJpaEntity();
        entity.setId(domain.getId());
        entity.setTenantId(domain.getTenantId());
        entity.setNome(domain.getNome());
        entity.setEmail(domain.getEmail());
        entity.setSenhaHash(domain.getSenhaHash());
        entity.setTelefoneCelular(domain.getTelefoneCelular());
        entity.setRole(domain.getRole().name());
        entity.setAtivo(domain.isAtivo());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());

        return entity;
    }
}
