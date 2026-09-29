package br.com.govflow.core.application.service;

import br.com.govflow.core.application.port.in.GerenciarAgenteUseCase;
import br.com.govflow.core.application.port.out.PrefeituraRepositoryPort;
import br.com.govflow.core.application.port.out.UsuarioRepositoryPort;
import br.com.govflow.core.domain.exception.AcessoNegadoException;
import br.com.govflow.core.domain.exception.EmailJaCadastradoException;
import br.com.govflow.core.domain.exception.PrefeituraNaoEncontradaException;
import br.com.govflow.core.domain.exception.UsuarioNaoEncontradoException;
import br.com.govflow.core.domain.model.Prefeitura;
import br.com.govflow.core.domain.model.usuario.RoleUsuario;
import br.com.govflow.core.domain.model.usuario.Usuario;
import br.com.govflow.core.infrastructure.error.MissingTenantHeaderException;
import br.com.govflow.core.infrastructure.interceptor.TenantContext;
import br.com.govflow.core.infrastructure.interceptor.UserContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class AgenteService implements GerenciarAgenteUseCase {

    private final UsuarioRepositoryPort usuarioRepository;
    private final PrefeituraRepositoryPort prefeituraRepository;
    private final PasswordEncoder passwordEncoder;

    public AgenteService(
            UsuarioRepositoryPort usuarioRepository,
            PrefeituraRepositoryPort prefeituraRepository,
            PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.prefeituraRepository = prefeituraRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public AgenteResponse criar(CriarAgenteCommand command) {
        validarPermissaoAdmin();
        UUID tenantId = obterTenantId();

        String emailNorm = command.email().trim().toLowerCase();
        if (usuarioRepository.existePorEmail(emailNorm)) {
            throw new EmailJaCadastradoException(emailNorm);
        }

        String celularE164 = AuthService.formatarTelefoneE164(command.telefoneCelular());
        validarPrefeiturasDoTenant(command.prefeiturasIds(), tenantId);

        String senhaHash = passwordEncoder.encode(command.senha());

        Usuario agente = Usuario.criarNovoAgente(
                tenantId,
                command.nome(),
                emailNorm,
                senhaHash,
                celularE164,
                command.prefeiturasIds()
        );

        Usuario agenteSalvo = usuarioRepository.salvar(agente);
        return toResponse(agenteSalvo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgenteResponse> listar() {
        validarPermissaoAdmin();
        UUID tenantId = obterTenantId();
        return usuarioRepository.listarAgentesPorTenant(tenantId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AgenteResponse buscarPorId(UUID id) {
        validarPermissaoAdmin();
        UUID tenantId = obterTenantId();
        Usuario agente = usuarioRepository.buscarPorIdETenantId(id, tenantId)
                .orElseThrow(() -> new UsuarioNaoEncontradoException(id));
        return toResponse(agente);
    }

    @Override
    public AgenteResponse atualizar(AtualizarAgenteCommand command) {
        validarPermissaoAdmin();
        UUID tenantId = obterTenantId();

        Usuario agente = usuarioRepository.buscarPorIdETenantId(command.id(), tenantId)
                .orElseThrow(() -> new UsuarioNaoEncontradoException(command.id()));

        String celularE164 = AuthService.formatarTelefoneE164(command.telefoneCelular());
        agente.atualizarDados(command.nome(), celularE164);

        if (command.ativo() != null) {
            if (command.ativo()) {
                agente.ativar();
            } else {
                agente.inativar();
            }
        }

        if (command.prefeiturasIds() != null) {
            validarPrefeiturasDoTenant(command.prefeiturasIds(), tenantId);
            agente.atribuirPrefeituras(command.prefeiturasIds());
        }

        Usuario atualizado = usuarioRepository.salvar(agente);
        return toResponse(atualizado);
    }

    @Override
    public void inativar(UUID id) {
        validarPermissaoAdmin();
        UUID tenantId = obterTenantId();

        Usuario agente = usuarioRepository.buscarPorIdETenantId(id, tenantId)
                .orElseThrow(() -> new UsuarioNaoEncontradoException(id));

        agente.inativar();
        usuarioRepository.salvar(agente);
    }

    private void validarPermissaoAdmin() {
        // Se houver roles definidas e não contiver ADMIN, bloqueia
        Set<String> roles = UserContext.getRoles();
        if (!roles.isEmpty() && !UserContext.isAdmin()) {
            throw new AcessoNegadoException("Apenas administradores podem gerenciar agentes da equipe.");
        }
    }

    private UUID obterTenantId() {
        UUID tenantId = UserContext.getTenantId();
        if (tenantId == null) {
            tenantId = TenantContext.getCurrentTenant();
        }
        if (tenantId == null) {
            throw new MissingTenantHeaderException("Tenant não identificado na requisição.");
        }
        return tenantId;
    }

    private void validarPrefeiturasDoTenant(Set<UUID> prefeiturasIds, UUID tenantId) {
        if (prefeiturasIds == null || prefeiturasIds.isEmpty()) {
            return;
        }
        for (UUID prefId : prefeiturasIds) {
            Prefeitura prefeitura = prefeituraRepository.buscarPorId(prefId)
                    .orElseThrow(() -> new PrefeituraNaoEncontradaException(prefId));
            if (!Objects.equals(prefeitura.getTenantId(), tenantId)) {
                throw new AcessoNegadoException("A prefeitura selecionada não pertence a esta consultoria.");
            }
        }
    }

    private AgenteResponse toResponse(Usuario usuario) {
        return new AgenteResponse(
                usuario.getId(),
                usuario.getTenantId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getTelefoneCelular(),
                usuario.getRole(),
                usuario.isAtivo(),
                usuario.getPrefeiturasAtribuidasIds(),
                usuario.getCreatedAt(),
                usuario.getUpdatedAt()
        );
    }
}
