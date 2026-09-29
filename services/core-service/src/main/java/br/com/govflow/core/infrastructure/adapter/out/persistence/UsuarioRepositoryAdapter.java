package br.com.govflow.core.infrastructure.adapter.out.persistence;

import br.com.govflow.core.application.port.out.UsuarioRepositoryPort;
import br.com.govflow.core.domain.model.usuario.RoleUsuario;
import br.com.govflow.core.domain.model.usuario.Usuario;
import br.com.govflow.core.infrastructure.adapter.out.persistence.entity.UsuarioJpaEntity;
import br.com.govflow.core.infrastructure.adapter.out.persistence.entity.UsuarioPrefeituraJpaEntity;
import br.com.govflow.core.infrastructure.adapter.out.persistence.mapper.UsuarioPersistenceMapper;
import br.com.govflow.core.infrastructure.adapter.out.persistence.repository.SpringDataUsuarioPrefeituraRepository;
import br.com.govflow.core.infrastructure.adapter.out.persistence.repository.SpringDataUsuarioRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class UsuarioRepositoryAdapter implements UsuarioRepositoryPort {

    private final SpringDataUsuarioRepository usuarioRepository;
    private final SpringDataUsuarioPrefeituraRepository usuarioPrefeituraRepository;
    private final UsuarioPersistenceMapper mapper;

    public UsuarioRepositoryAdapter(
            SpringDataUsuarioRepository usuarioRepository,
            SpringDataUsuarioPrefeituraRepository usuarioPrefeituraRepository,
            UsuarioPersistenceMapper mapper) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioPrefeituraRepository = usuarioPrefeituraRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Usuario salvar(Usuario usuario) {
        UsuarioJpaEntity entity = mapper.toEntity(usuario);
        UsuarioJpaEntity saved = usuarioRepository.save(entity);

        // Atualizar vínculos de prefeituras se for AGENTE
        if (usuario.getRole() == RoleUsuario.AGENTE && usuario.getPrefeiturasAtribuidasIds() != null) {
            atualizarPrefeiturasAtribuidas(usuario.getId(), usuario.getPrefeiturasAtribuidasIds());
        }

        Set<UUID> prefeituras = buscarPrefeiturasAtribuidas(saved.getId());
        return mapper.toDomain(saved, prefeituras);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorId(UUID id) {
        return usuarioRepository.findById(id)
                .map(entity -> mapper.toDomain(entity, buscarPrefeiturasAtribuidas(entity.getId())));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorIdETenantId(UUID id, UUID tenantId) {
        return usuarioRepository.findByIdAndTenantId(id, tenantId)
                .map(entity -> mapper.toDomain(entity, buscarPrefeiturasAtribuidas(entity.getId())));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorEmail(String email) {
        if (email == null) return Optional.empty();
        return usuarioRepository.findByEmail(email.trim().toLowerCase())
                .map(entity -> mapper.toDomain(entity, buscarPrefeiturasAtribuidas(entity.getId())));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorEmailETenantId(String email, UUID tenantId) {
        if (email == null) return Optional.empty();
        return usuarioRepository.findByEmailAndTenantId(email.trim().toLowerCase(), tenantId)
                .map(entity -> mapper.toDomain(entity, buscarPrefeiturasAtribuidas(entity.getId())));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorTelefoneCelular(String telefoneCelular) {
        if (telefoneCelular == null || telefoneCelular.isBlank()) return Optional.empty();
        return usuarioRepository.findByTelefoneCelular(telefoneCelular.trim())
                .map(entity -> mapper.toDomain(entity, buscarPrefeiturasAtribuidas(entity.getId())));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Usuario> listarPorTenant(UUID tenantId) {
        return usuarioRepository.findByTenantId(tenantId).stream()
                .map(entity -> mapper.toDomain(entity, buscarPrefeiturasAtribuidas(entity.getId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Usuario> listarAgentesPorTenant(UUID tenantId) {
        return usuarioRepository.findByTenantIdAndRole(tenantId, RoleUsuario.AGENTE.name()).stream()
                .map(entity -> mapper.toDomain(entity, buscarPrefeiturasAtribuidas(entity.getId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existePorEmail(String email) {
        if (email == null) return false;
        return usuarioRepository.existsByEmail(email.trim().toLowerCase());
    }

    @Override
    @Transactional(readOnly = true)
    public Set<UUID> buscarPrefeiturasAtribuidas(UUID usuarioId) {
        if (usuarioId == null) return Collections.emptySet();
        return usuarioPrefeituraRepository.findByUsuarioId(usuarioId).stream()
                .map(UsuarioPrefeituraJpaEntity::getPrefeituraId)
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional
    public void atualizarPrefeiturasAtribuidas(UUID usuarioId, Set<UUID> prefeiturasIds) {
        usuarioPrefeituraRepository.deleteByUsuarioId(usuarioId);
        if (prefeiturasIds != null && !prefeiturasIds.isEmpty()) {
            Instant now = Instant.now();
            List<UsuarioPrefeituraJpaEntity> entities = prefeiturasIds.stream()
                    .map(prefId -> new UsuarioPrefeituraJpaEntity(usuarioId, prefId, now))
                    .collect(Collectors.toList());
            usuarioPrefeituraRepository.saveAll(entities);
        }
    }
}
