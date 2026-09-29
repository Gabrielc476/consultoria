package br.com.govflow.core.application.service;

import br.com.govflow.core.application.port.in.AutenticarUsuarioUseCase;
import br.com.govflow.core.application.port.in.CadastrarConsultoriaComAdminUseCase;
import br.com.govflow.core.application.port.in.ObterUsuarioAutenticadoUseCase;
import br.com.govflow.core.application.port.out.ConsultoriaRepositoryPort;
import br.com.govflow.core.application.port.out.UsuarioRepositoryPort;
import br.com.govflow.core.domain.exception.ConsultoriaJaCadastradaException;
import br.com.govflow.core.domain.exception.CredenciaisInvalidasException;
import br.com.govflow.core.domain.exception.EmailJaCadastradoException;
import br.com.govflow.core.domain.exception.UsuarioInativoException;
import br.com.govflow.core.domain.exception.UsuarioNaoEncontradoException;
import br.com.govflow.core.domain.model.Cnpj;
import br.com.govflow.core.domain.model.Consultoria;
import br.com.govflow.core.domain.model.usuario.RoleUsuario;
import br.com.govflow.core.domain.model.usuario.Usuario;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class AuthService implements AutenticarUsuarioUseCase, CadastrarConsultoriaComAdminUseCase, ObterUsuarioAutenticadoUseCase {

    private final UsuarioRepositoryPort usuarioRepository;
    private final ConsultoriaRepositoryPort consultoriaRepository;
    private final PasswordEncoder passwordEncoder;
    private final String jwtSecret;

    public AuthService(
            UsuarioRepositoryPort usuarioRepository,
            ConsultoriaRepositoryPort consultoriaRepository,
            PasswordEncoder passwordEncoder,
            @Value("${govflow.jwt.secret:${JWT_SECRET:GovFlowLocalDevJwtSecretKeyChangeMe32b!}}") String jwtSecret) {
        this.usuarioRepository = usuarioRepository;
        this.consultoriaRepository = consultoriaRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtSecret = jwtSecret;
    }

    @Override
    public UsuarioAutenticado autenticar(AutenticarUsuarioCommand command) {
        if (command == null || command.email() == null || command.senha() == null) {
            throw new CredenciaisInvalidasException();
        }

        String emailNorm = command.email().trim().toLowerCase();
        Usuario usuario = usuarioRepository.buscarPorEmail(emailNorm)
                .orElseThrow(CredenciaisInvalidasException::new);

        if (!passwordEncoder.matches(command.senha(), usuario.getSenhaHash())) {
            throw new CredenciaisInvalidasException();
        }

        if (!usuario.isAtivo()) {
            throw new UsuarioInativoException();
        }

        String nomeConsultoria = consultoriaRepository.buscarPorId(usuario.getTenantId())
                .map(c -> c.getNomeFantasia() != null && !c.getNomeFantasia().isBlank()
                        ? c.getNomeFantasia()
                        : c.getRazaoSocial())
                .orElse("GovFlow Consultoria");

        String token = gerarTokenJwt(usuario);

        return new UsuarioAutenticado(
                token,
                "Bearer",
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getTenantId(),
                nomeConsultoria,
                usuario.getRole(),
                usuario.getPrefeiturasAtribuidasIds()
        );
    }

    @Override
    public UsuarioAutenticado cadastrar(CadastrarConsultoriaComAdminCommand command) {
        Cnpj cnpj = new Cnpj(command.cnpj());
        if (consultoriaRepository.existePorCnpj(cnpj)) {
            throw new ConsultoriaJaCadastradaException(cnpj);
        }

        String emailNorm = command.emailAdmin().trim().toLowerCase();
        if (usuarioRepository.existePorEmail(emailNorm)) {
            throw new EmailJaCadastradoException(emailNorm);
        }

        String telefoneContato = formatarTelefoneE164(command.telefoneContato());
        String celularAdmin = formatarTelefoneE164(command.celularAdmin() != null ? command.celularAdmin() : command.telefoneContato());
        String nomeFantasia = (command.nomeFantasia() != null && !command.nomeFantasia().isBlank())
                ? command.nomeFantasia().trim()
                : command.razaoSocial().trim();

        Consultoria consultoria = Consultoria.criarNova(
                cnpj,
                command.razaoSocial(),
                nomeFantasia,
                command.emailContato(),
                telefoneContato,
                command.plano()
        );
        Consultoria consultoriaSalva = consultoriaRepository.salvar(consultoria);

        String senhaHash = passwordEncoder.encode(command.senhaAdmin());
        Usuario admin = Usuario.criarNovoAdmin(
                consultoriaSalva.getId(),
                command.nomeAdmin(),
                emailNorm,
                senhaHash,
                celularAdmin
        );
        Usuario adminSalvo = usuarioRepository.salvar(admin);

        String nomeConsultoria = consultoriaSalva.getNomeFantasia() != null && !consultoriaSalva.getNomeFantasia().isBlank()
                ? consultoriaSalva.getNomeFantasia()
                : consultoriaSalva.getRazaoSocial();

        String token = gerarTokenJwt(adminSalvo);

        return new UsuarioAutenticado(
                token,
                "Bearer",
                adminSalvo.getId(),
                adminSalvo.getNome(),
                adminSalvo.getEmail(),
                adminSalvo.getTenantId(),
                nomeConsultoria,
                adminSalvo.getRole(),
                adminSalvo.getPrefeiturasAtribuidasIds()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioAutenticado obterPerfil(UUID usuarioId, UUID tenantId) {
        Usuario usuario = usuarioRepository.buscarPorIdETenantId(usuarioId, tenantId)
                .orElseThrow(() -> new UsuarioNaoEncontradoException(usuarioId));

        String nomeConsultoria = consultoriaRepository.buscarPorId(usuario.getTenantId())
                .map(c -> c.getNomeFantasia() != null && !c.getNomeFantasia().isBlank()
                        ? c.getNomeFantasia()
                        : c.getRazaoSocial())
                .orElse("GovFlow Consultoria");

        String token = gerarTokenJwt(usuario);

        return new UsuarioAutenticado(
                token,
                "Bearer",
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getTenantId(),
                nomeConsultoria,
                usuario.getRole(),
                usuario.getPrefeiturasAtribuidasIds()
        );
    }

    public String gerarTokenJwt(Usuario usuario) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        Instant exp = now.plus(24, ChronoUnit.HOURS);

        return Jwts.builder()
                .subject(usuario.getId().toString())
                .claim("tenant_id", usuario.getTenantId().toString())
                .claim("user_id", usuario.getId().toString())
                .claim("username", usuario.getEmail())
                .claim("roles", List.of(usuario.getRole().name()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    public static String formatarTelefoneE164(String telefone) {
        if (telefone == null || telefone.isBlank()) {
            return null;
        }
        String clean = telefone.replaceAll("[^0-9+]", "");
        if (clean.isBlank()) {
            return null;
        }
        if (clean.startsWith("+")) {
            return clean;
        }
        if (clean.startsWith("55") && (clean.length() == 12 || clean.length() == 13)) {
            return "+" + clean;
        }
        if (clean.length() == 10 || clean.length() == 11) {
            return "+55" + clean;
        }
        return "+" + clean;
    }
}
