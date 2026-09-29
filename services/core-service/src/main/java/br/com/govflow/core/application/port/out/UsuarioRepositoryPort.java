package br.com.govflow.core.application.port.out;

import br.com.govflow.core.domain.model.usuario.Usuario;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface UsuarioRepositoryPort {

    Usuario salvar(Usuario usuario);

    Optional<Usuario> buscarPorId(UUID id);

    Optional<Usuario> buscarPorIdETenantId(UUID id, UUID tenantId);

    Optional<Usuario> buscarPorEmail(String email);

    Optional<Usuario> buscarPorEmailETenantId(String email, UUID tenantId);

    Optional<Usuario> buscarPorTelefoneCelular(String telefoneCelular);

    List<Usuario> listarPorTenant(UUID tenantId);

    List<Usuario> listarAgentesPorTenant(UUID tenantId);

    boolean existePorEmail(String email);

    Set<UUID> buscarPrefeiturasAtribuidas(UUID usuarioId);

    void atualizarPrefeiturasAtribuidas(UUID usuarioId, Set<UUID> prefeiturasIds);
}
