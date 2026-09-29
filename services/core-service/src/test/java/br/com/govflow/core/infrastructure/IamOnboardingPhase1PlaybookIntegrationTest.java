package br.com.govflow.core.infrastructure;

import br.com.govflow.core.application.port.in.AutenticarUsuarioUseCase;
import br.com.govflow.core.application.port.in.AutenticarUsuarioUseCase.AutenticarUsuarioCommand;
import br.com.govflow.core.application.port.in.AutenticarUsuarioUseCase.UsuarioAutenticado;
import br.com.govflow.core.application.port.in.CadastrarConsultoriaComAdminUseCase;
import br.com.govflow.core.application.port.in.CadastrarConsultoriaComAdminUseCase.CadastrarConsultoriaComAdminCommand;
import br.com.govflow.core.application.port.in.CadastrarPrefeituraUseCase;
import br.com.govflow.core.application.port.in.CadastrarPrefeituraUseCase.CadastrarPrefeituraCommand;
import br.com.govflow.core.application.port.in.ConsultarPrefeituraUseCase;
import br.com.govflow.core.application.port.in.GerenciarAgenteUseCase;
import br.com.govflow.core.application.port.in.GerenciarAgenteUseCase.AgenteResponse;
import br.com.govflow.core.application.port.in.GerenciarAgenteUseCase.CriarAgenteCommand;
import br.com.govflow.core.application.port.out.UsuarioRepositoryPort;
import br.com.govflow.core.domain.exception.AcessoNegadoException;
import br.com.govflow.core.domain.exception.CredenciaisInvalidasException;
import br.com.govflow.core.domain.model.PlanoConsultoria;
import br.com.govflow.core.domain.model.PorteMunicipio;
import br.com.govflow.core.domain.model.Prefeitura;
import br.com.govflow.core.domain.model.usuario.RoleUsuario;
import br.com.govflow.core.domain.model.usuario.Usuario;
import br.com.govflow.core.infrastructure.adapter.out.persistence.repository.SpringDataConsultoriaRepository;
import br.com.govflow.core.infrastructure.adapter.out.persistence.repository.SpringDataPrefeituraRepository;
import br.com.govflow.core.infrastructure.adapter.out.persistence.repository.SpringDataUsuarioPrefeituraRepository;
import br.com.govflow.core.infrastructure.adapter.out.persistence.repository.SpringDataUsuarioRepository;
import br.com.govflow.core.infrastructure.interceptor.TenantContext;
import br.com.govflow.core.infrastructure.interceptor.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class IamOnboardingPhase1PlaybookIntegrationTest {

    @Autowired
    private CadastrarConsultoriaComAdminUseCase cadastrarConsultoriaComAdminUseCase;

    @Autowired
    private AutenticarUsuarioUseCase autenticarUsuarioUseCase;

    @Autowired
    private GerenciarAgenteUseCase gerenciarAgenteUseCase;

    @Autowired
    private CadastrarPrefeituraUseCase cadastrarPrefeituraUseCase;

    @Autowired
    private ConsultarPrefeituraUseCase consultarPrefeituraUseCase;

    @Autowired
    private UsuarioRepositoryPort usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SpringDataUsuarioPrefeituraRepository usuarioPrefeituraRepository;

    @Autowired
    private SpringDataUsuarioRepository usuarioJpaRepository;

    @Autowired
    private SpringDataPrefeituraRepository prefeituraJpaRepository;

    @Autowired
    private SpringDataConsultoriaRepository consultoriaJpaRepository;

    @BeforeEach
    @AfterEach
    void cleanup() {
        UserContext.clear();
        TenantContext.clear();
        usuarioPrefeituraRepository.deleteAll();
        usuarioJpaRepository.deleteAll();
        prefeituraJpaRepository.deleteAll();
        consultoriaJpaRepository.deleteAll();
    }

    @Test
    @DisplayName("Playbook da Fase 1 (Passos 1 ao 5): Fluxo Completo de IAM, Persistência BCrypt, Agentes e Escopo")
    void deveExecutarPlaybookManualFase1CompletoComSucesso() {
        // =========================================================================
        // PASSO 1: Cadastro da Consultoria e Administrador
        // =========================================================================
        CadastrarConsultoriaComAdminCommand cmdCadastro = new CadastrarConsultoriaComAdminCommand(
                "13.519.354/0001-99",
                "Planeja Brasil Gestão Pública LTDA",
                "Planeja Brasil Consultoria",
                "contato@planejabrasil.com.br",
                "(83) 3421-2000",
                PlanoConsultoria.PRO,
                "Carlos Gestor",
                "carlos@planejabrasil.com.br",
                "GovFlow2026!",
                "(83) 99999-8888"
        );

        UsuarioAutenticado adminAutenticado = cadastrarConsultoriaComAdminUseCase.cadastrar(cmdCadastro);
        assertNotNull(adminAutenticado, "O retorno do cadastro deve conter os dados autenticados");
        assertNotNull(adminAutenticado.token(), "O JWT deve ter sido emitido");
        UUID tenantId = adminAutenticado.tenantId();
        UUID adminId = adminAutenticado.id();
        assertNotNull(tenantId);
        assertNotNull(adminId);

        // =========================================================================
        // PASSO 2: Verificação de Banco de Dados (BCrypt cost 12 e Role ADMIN)
        // =========================================================================
        Optional<Usuario> adminOpt = usuarioRepository.buscarPorEmail("carlos@planejabrasil.com.br");
        assertTrue(adminOpt.isPresent(), "O usuário administrador deve estar persistido no banco");
        Usuario adminNoBanco = adminOpt.get();
        assertEquals(RoleUsuario.ADMIN, adminNoBanco.getRole());
        assertEquals("+5583999998888", adminNoBanco.getTelefoneCelular(), "Celular deve estar normalizado E.164");
        assertTrue(adminNoBanco.getSenhaHash().startsWith("$2a$12$"), "Hash de senha deve ser BCrypt com custo 12");
        assertTrue(passwordEncoder.matches("GovFlow2026!", adminNoBanco.getSenhaHash()), "Senha BCrypt deve bater com a original");

        // =========================================================================
        // PASSO 3: Teste de Autenticação & Validação de Senha
        // =========================================================================
        // Tentativa 1: Senha incorreta ("123456") -> Deve rejeitar
        AutenticarUsuarioCommand cmdSenhaErrada = new AutenticarUsuarioCommand("carlos@planejabrasil.com.br", "123456");
        assertThrows(CredenciaisInvalidasException.class, () -> autenticarUsuarioUseCase.autenticar(cmdSenhaErrada),
                "Autenticação com senha errada deve disparar CredenciaisInvalidasException");

        // Tentativa 2: Senha correta ("GovFlow2026!") -> Sucesso e retorno de token
        AutenticarUsuarioCommand cmdSenhaCorreta = new AutenticarUsuarioCommand("carlos@planejabrasil.com.br", "GovFlow2026!");
        UsuarioAutenticado loginOk = autenticarUsuarioUseCase.autenticar(cmdSenhaCorreta);
        assertNotNull(loginOk.token());
        assertEquals(adminId, loginOk.id());
        assertEquals(RoleUsuario.ADMIN, loginOk.role());

        // =========================================================================
        // PASSO 4: Cadastro de Agente e Alocação de Prefeituras
        // =========================================================================
        // 4.1. Configura contexto do ADMIN da consultoria para cadastrar 2 prefeituras
        TenantContext.setCurrentTenant(tenantId);
        UserContext.setCurrentUser(adminId, tenantId, Set.of("ADMIN"), Set.of());

        // Cadastra Prefeitura 1 (João Pessoa - PB)
        Prefeitura prefPatos = cadastrarPrefeituraUseCase.cadastrar(new CadastrarPrefeituraCommand(
                tenantId,
                "08.778.326/0001-56",
                "Prefeitura Municipal de João Pessoa",
                "João Pessoa",
                "PB",
                "2507507",
                PorteMunicipio.GRANDE_PORTE,
                "Prefeito João Pessoa",
                null,
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2028, 12, 31)
        ));

        // Cadastra Prefeitura 2 (Pombal - PB)
        Prefeitura prefSousa = cadastrarPrefeituraUseCase.cadastrar(new CadastrarPrefeituraCommand(
                tenantId,
                "08.923.456/0002-16",
                "Prefeitura Municipal de Pombal",
                "Pombal",
                "PB",
                "2512101",
                PorteMunicipio.MEDIO_PORTE,
                "Prefeito Pombal",
                null,
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2028, 12, 31)
        ));

        // 4.2. ADMIN cria um Agente (João Analista) com celular e atribui APENAS Patos
        CriarAgenteCommand cmdCriarAgente = new CriarAgenteCommand(
                "João Analista",
                "joao@planejabrasil.com.br",
                "GovFlowTemp2026!",
                "(83) 98888-1111",
                Set.of(prefPatos.getId())
        );

        AgenteResponse agenteCriado = gerenciarAgenteUseCase.criar(cmdCriarAgente);
        assertNotNull(agenteCriado);
        assertNotNull(agenteCriado.id());
        assertEquals(RoleUsuario.AGENTE, agenteCriado.role());
        assertEquals("+5583988881111", agenteCriado.telefoneCelular());
        assertEquals(1, agenteCriado.prefeiturasAtribuidasIds().size());
        assertTrue(agenteCriado.prefeiturasAtribuidasIds().contains(prefPatos.getId()));

        // =========================================================================
        // PASSO 5: Validação de Escopo do Agente
        // =========================================================================
        // 5.1. Contexto muda para o AGENTE João Analista
        UserContext.setCurrentUser(agenteCriado.id(), tenantId, Set.of("AGENTE"), Set.of(prefPatos.getId()));

        // 5.2. Listar prefeituras como Agente: deve retornar APENAS Patos
        List<Prefeitura> prefeiturasDoAgente = consultarPrefeituraUseCase.listar(0, 10, true);
        assertEquals(1, prefeiturasDoAgente.size(), "O agente deve enxergar exatamente 1 prefeitura");
        assertEquals(prefPatos.getId(), prefeiturasDoAgente.get(0).getId());

        // 5.3. Agente tentando buscar a prefeitura de Patos (permitida): Sucesso
        Optional<Prefeitura> patosOpt = consultarPrefeituraUseCase.buscarPorId(prefPatos.getId());
        assertTrue(patosOpt.isPresent());

        // 5.4. Agente tentando acessar a prefeitura de Sousa (não permitida): Acesso Negado (403)
        assertThrows(AcessoNegadoException.class, () -> consultarPrefeituraUseCase.buscarPorId(prefSousa.getId()),
                "O agente não deve ter permissão para acessar prefeitura não vinculada");

        // 5.5. Contexto volta para o ADMIN: enxerga TODAS as prefeituras da consultoria (Patos e Sousa)
        UserContext.setCurrentUser(adminId, tenantId, Set.of("ADMIN"), Set.of());
        List<Prefeitura> prefeiturasDoAdmin = consultarPrefeituraUseCase.listar(0, 10, true);
        assertEquals(2, prefeiturasDoAdmin.size(), "O administrador deve enxergar todas as prefeituras cadastradas");
    }
}
