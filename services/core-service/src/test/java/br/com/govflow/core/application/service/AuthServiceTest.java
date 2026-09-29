package br.com.govflow.core.application.service;

import br.com.govflow.core.application.port.in.AutenticarUsuarioUseCase;
import br.com.govflow.core.application.port.in.CadastrarConsultoriaComAdminUseCase;
import br.com.govflow.core.application.port.out.ConsultoriaRepositoryPort;
import br.com.govflow.core.application.port.out.UsuarioRepositoryPort;
import br.com.govflow.core.domain.exception.ConsultoriaJaCadastradaException;
import br.com.govflow.core.domain.exception.CredenciaisInvalidasException;
import br.com.govflow.core.domain.exception.EmailJaCadastradoException;
import br.com.govflow.core.domain.exception.UsuarioInativoException;
import br.com.govflow.core.domain.model.Cnpj;
import br.com.govflow.core.domain.model.Consultoria;
import br.com.govflow.core.domain.model.PlanoConsultoria;
import br.com.govflow.core.domain.model.usuario.RoleUsuario;
import br.com.govflow.core.domain.model.usuario.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepositoryPort usuarioRepository;

    @Mock
    private ConsultoriaRepositoryPort consultoriaRepository;

    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    private final String jwtSecret = "GovFlowLocalDevJwtSecretKeyChangeMe32b!";

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(12);
        authService = new AuthService(usuarioRepository, consultoriaRepository, passwordEncoder, jwtSecret);
    }

    @Test
    @DisplayName("Deve autenticar com sucesso quando e-mail e senha conferem via BCrypt")
    void deveAutenticarComSucesso() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        String rawPassword = "GovFlow2026!";
        String hash = passwordEncoder.encode(rawPassword);

        Usuario usuario = new Usuario(
                userId,
                tenantId,
                "Carlos Gestor",
                "gestor@planejabrasil.com.br",
                hash,
                "+5583999998888",
                RoleUsuario.ADMIN,
                true,
                Collections.emptySet(),
                null,
                null
        );

        Consultoria consultoria = Consultoria.criarNova(
                new Cnpj("13.519.354/0001-99"),
                "Planeja Brasil",
                "Planeja Brasil",
                "gestor@planejabrasil.com.br",
                "+5583999998888",
                PlanoConsultoria.PRO
        );

        when(usuarioRepository.buscarPorEmail(eq("gestor@planejabrasil.com.br"))).thenReturn(Optional.of(usuario));
        when(consultoriaRepository.buscarPorId(eq(tenantId))).thenReturn(Optional.of(consultoria));

        var result = authService.autenticar(new AutenticarUsuarioUseCase.AutenticarUsuarioCommand(
                "gestor@planejabrasil.com.br",
                rawPassword
        ));

        assertNotNull(result);
        assertNotNull(result.token());
        assertEquals("Bearer", result.tokenType());
        assertEquals(userId, result.id());
        assertEquals(RoleUsuario.ADMIN, result.role());
    }

    @Test
    @DisplayName("Deve falhar autenticação quando a senha for incorreta")
    void deveFalharQuandoSenhaIncorreta() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        String hash = passwordEncoder.encode("GovFlow2026!");

        Usuario usuario = new Usuario(
                userId,
                tenantId,
                "Carlos Gestor",
                "gestor@planejabrasil.com.br",
                hash,
                "+5583999998888",
                RoleUsuario.ADMIN,
                true,
                Collections.emptySet(),
                null,
                null
        );

        when(usuarioRepository.buscarPorEmail(eq("gestor@planejabrasil.com.br"))).thenReturn(Optional.of(usuario));

        assertThrows(CredenciaisInvalidasException.class, () ->
                authService.autenticar(new AutenticarUsuarioUseCase.AutenticarUsuarioCommand(
                        "gestor@planejabrasil.com.br",
                        "senhaErrada"
                )));
    }

    @Test
    @DisplayName("Deve falhar autenticação quando o usuário estiver inativo")
    void deveFalharQuandoUsuarioInativo() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        String hash = passwordEncoder.encode("GovFlow2026!");

        Usuario usuario = new Usuario(
                userId,
                tenantId,
                "Carlos Gestor",
                "gestor@planejabrasil.com.br",
                hash,
                "+5583999998888",
                RoleUsuario.ADMIN,
                false, // Inativo
                Collections.emptySet(),
                null,
                null
        );

        when(usuarioRepository.buscarPorEmail(eq("gestor@planejabrasil.com.br"))).thenReturn(Optional.of(usuario));

        assertThrows(UsuarioInativoException.class, () ->
                authService.autenticar(new AutenticarUsuarioUseCase.AutenticarUsuarioCommand(
                        "gestor@planejabrasil.com.br",
                        "GovFlow2026!"
                )));
    }

    @Test
    @DisplayName("Deve cadastrar consultoria e persistir usuário ADMIN com BCrypt")
    void deveCadastrarConsultoriaEAdminComBCrypt() {
        CadastrarConsultoriaComAdminUseCase.CadastrarConsultoriaComAdminCommand command =
                new CadastrarConsultoriaComAdminUseCase.CadastrarConsultoriaComAdminCommand(
                        "13.519.354/0001-99",
                        "Planeja Brasil Consultoria",
                        "Planeja Brasil",
                        "gestor@planejabrasil.com.br",
                        "(83) 99999-8888",
                        PlanoConsultoria.PRO,
                        "Carlos Silva",
                        "gestor@planejabrasil.com.br",
                        "GovFlow2026!",
                        "(83) 99999-8888"
                );

        when(consultoriaRepository.existePorCnpj(any())).thenReturn(false);
        when(usuarioRepository.existePorEmail(any())).thenReturn(false);
        when(consultoriaRepository.salvar(any(Consultoria.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(usuarioRepository.salvar(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = authService.cadastrar(command);

        assertNotNull(response);
        assertNotNull(response.token());
        assertEquals("gestor@planejabrasil.com.br", response.email());
        assertEquals(RoleUsuario.ADMIN, response.role());
        verify(usuarioRepository, times(1)).salvar(argThat(u ->
                passwordEncoder.matches("GovFlow2026!", u.getSenhaHash()) && u.getRole() == RoleUsuario.ADMIN
        ));
    }
}
