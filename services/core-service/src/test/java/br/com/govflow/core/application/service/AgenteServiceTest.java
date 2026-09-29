package br.com.govflow.core.application.service;

import br.com.govflow.core.application.port.in.GerenciarAgenteUseCase;
import br.com.govflow.core.application.port.out.PrefeituraRepositoryPort;
import br.com.govflow.core.application.port.out.UsuarioRepositoryPort;
import br.com.govflow.core.domain.exception.AcessoNegadoException;
import br.com.govflow.core.domain.exception.EmailJaCadastradoException;
import br.com.govflow.core.domain.model.Cnpj;
import br.com.govflow.core.domain.model.CodigoIbge;
import br.com.govflow.core.domain.model.PorteMunicipio;
import br.com.govflow.core.domain.model.Prefeitura;
import br.com.govflow.core.domain.model.Uf;
import br.com.govflow.core.domain.model.usuario.RoleUsuario;
import br.com.govflow.core.domain.model.usuario.Usuario;
import br.com.govflow.core.infrastructure.interceptor.TenantContext;
import br.com.govflow.core.infrastructure.interceptor.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgenteServiceTest {

    @Mock
    private UsuarioRepositoryPort usuarioRepository;

    @Mock
    private PrefeituraRepositoryPort prefeituraRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AgenteService agenteService;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setCurrentTenant(tenantId);
        UserContext.setCurrentUser(UUID.randomUUID(), tenantId, Set.of("ADMIN"), Collections.emptySet());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        UserContext.clear();
    }

    @Test
    @DisplayName("Deve criar agente com celular e prefeituras com sucesso")
    void deveCriarAgenteComSucesso() {
        UUID prefId = UUID.randomUUID();
        GerenciarAgenteUseCase.CriarAgenteCommand command = new GerenciarAgenteUseCase.CriarAgenteCommand(
                "João Analista",
                "joao@planejabrasil.com.br",
                "GovFlow2026!",
                "(83) 98888-1111",
                Set.of(prefId)
        );

        Prefeitura prefeitura = Prefeitura.criarNova(
                tenantId,
                new Cnpj("08.778.326/0001-56"),
                "Prefeitura Patos",
                "Patos",
                Uf.PB,
                new CodigoIbge("2510808"),
                PorteMunicipio.MEDIO_PORTE,
                "Prefeito",
                null,
                null,
                null
        );

        when(usuarioRepository.existePorEmail(eq("joao@planejabrasil.com.br"))).thenReturn(false);
        when(prefeituraRepository.buscarPorId(eq(prefId))).thenReturn(Optional.of(prefeitura));
        when(passwordEncoder.encode(eq("GovFlow2026!"))).thenReturn("$2a$12$hashedPassword");
        when(usuarioRepository.salvar(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = agenteService.criar(command);

        assertNotNull(response);
        assertEquals("João Analista", response.nome());
        assertEquals("joao@planejabrasil.com.br", response.email());
        assertEquals("+5583988881111", response.telefoneCelular());
        assertEquals(RoleUsuario.AGENTE, response.role());
        assertTrue(response.prefeiturasAtribuidasIds().contains(prefId));
        verify(usuarioRepository, times(1)).salvar(any(Usuario.class));
    }

    @Test
    @DisplayName("Deve rejeitar criação de agente por usuário não-ADMIN com 403 Forbidden")
    void deveRejeitarCriacaoPorNaoAdmin() {
        UserContext.setCurrentUser(UUID.randomUUID(), tenantId, Set.of("AGENTE"), Collections.emptySet());

        GerenciarAgenteUseCase.CriarAgenteCommand command = new GerenciarAgenteUseCase.CriarAgenteCommand(
                "João Analista",
                "joao@planejabrasil.com.br",
                "GovFlow2026!",
                "(83) 98888-1111",
                Collections.emptySet()
        );

        assertThrows(AcessoNegadoException.class, () -> agenteService.criar(command));
        verify(usuarioRepository, never()).salvar(any());
    }

    @Test
    @DisplayName("Deve falhar se e-mail do agente já estiver cadastrado")
    void deveFalharSeEmailJaCadastrado() {
        GerenciarAgenteUseCase.CriarAgenteCommand command = new GerenciarAgenteUseCase.CriarAgenteCommand(
                "João Analista",
                "duplicado@planejabrasil.com.br",
                "GovFlow2026!",
                "(83) 98888-1111",
                Collections.emptySet()
        );

        when(usuarioRepository.existePorEmail(eq("duplicado@planejabrasil.com.br"))).thenReturn(true);

        assertThrows(EmailJaCadastradoException.class, () -> agenteService.criar(command));
        verify(usuarioRepository, never()).salvar(any());
    }
}
