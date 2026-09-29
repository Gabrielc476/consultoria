package br.com.govflow.core.infrastructure.adapter.in.rest;

import br.com.govflow.core.application.port.in.AutenticarUsuarioUseCase;
import br.com.govflow.core.application.port.in.CadastrarConsultoriaComAdminUseCase;
import br.com.govflow.core.application.port.in.ObterUsuarioAutenticadoUseCase;
import br.com.govflow.core.domain.exception.CredenciaisInvalidasException;
import br.com.govflow.core.domain.model.PlanoConsultoria;
import br.com.govflow.core.domain.model.usuario.RoleUsuario;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.request.LoginRequest;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.request.RegisterConsultoriaRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AutenticarUsuarioUseCase autenticarUsuarioUseCase;

    @MockBean
    private CadastrarConsultoriaComAdminUseCase cadastrarConsultoriaComAdminUseCase;

    @MockBean
    private ObterUsuarioAutenticadoUseCase obterUsuarioAutenticadoUseCase;

    @Test
    @DisplayName("Deve realizar login com sucesso e retornar token JWT e dados do usuário")
    void deveRealizarLoginComSucesso() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        LoginRequest request = new LoginRequest("gestor@planejabrasil.com.br", "GovFlow2026!");

        var auth = new AutenticarUsuarioUseCase.UsuarioAutenticado(
                "mock-jwt-token",
                "Bearer",
                userId,
                "Carlos Gestor",
                "gestor@planejabrasil.com.br",
                tenantId,
                "Planeja Brasil",
                RoleUsuario.ADMIN,
                Collections.emptySet()
        );

        when(autenticarUsuarioUseCase.autenticar(any())).thenReturn(auth);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock-jwt-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.analistaId").value(userId.toString()))
                .andExpect(jsonPath("$.tenantId").value(tenantId.toString()))
                .andExpect(jsonPath("$.email").value("gestor@planejabrasil.com.br"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @DisplayName("Deve falhar com 401 quando as credenciais forem inválidas")
    void deveFalharSeCredenciaisInvalidas() throws Exception {
        LoginRequest request = new LoginRequest("gestor@planejabrasil.com.br", "senhaErrada");

        when(autenticarUsuarioUseCase.autenticar(any())).thenThrow(new CredenciaisInvalidasException());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Credenciais Inválidas"));
    }

    @Test
    @DisplayName("Deve falhar com 400 se e-mail estiver em formato inválido")
    void deveFalharSeEmailInvalido() throws Exception {
        LoginRequest request = new LoginRequest("email-invalido", "senha123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve cadastrar nova consultoria com ADMIN e retornar 201 com token JWT")
    void deveCadastrarConsultoriaComSucesso() throws Exception {
        RegisterConsultoriaRequest request = new RegisterConsultoriaRequest(
                "Planeja Brasil Consultoria",
                "Planeja Brasil",
                "13.519.354/0001-99",
                "carlos@planejabrasil.com.br",
                "GovFlow2026!",
                "Carlos Silva",
                "(83) 99999-8888",
                PlanoConsultoria.PRO
        );

        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        var auth = new AutenticarUsuarioUseCase.UsuarioAutenticado(
                "mock-jwt-token-created",
                "Bearer",
                userId,
                "Carlos Silva",
                "carlos@planejabrasil.com.br",
                tenantId,
                "Planeja Brasil",
                RoleUsuario.ADMIN,
                Collections.emptySet()
        );

        when(cadastrarConsultoriaComAdminUseCase.cadastrar(any())).thenReturn(auth);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("mock-jwt-token-created"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.analistaId").value(userId.toString()))
                .andExpect(jsonPath("$.nome").value("Carlos Silva"))
                .andExpect(jsonPath("$.email").value("carlos@planejabrasil.com.br"))
                .andExpect(jsonPath("$.tenantId").value(tenantId.toString()))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @DisplayName("Deve retornar perfil em /api/v1/auth/me quando headers estiverem presentes")
    void deveRetornarPerfilEmMe() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID prefId = UUID.randomUUID();

        var auth = new AutenticarUsuarioUseCase.UsuarioAutenticado(
                "mock-jwt-token-me",
                "Bearer",
                userId,
                "João Analista",
                "joao@planejabrasil.com.br",
                tenantId,
                "Planeja Brasil",
                RoleUsuario.AGENTE,
                Set.of(prefId)
        );

        when(obterUsuarioAutenticadoUseCase.obterPerfil(eq(userId), eq(tenantId))).thenReturn(auth);

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Roles", "AGENTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analistaId").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("joao@planejabrasil.com.br"))
                .andExpect(jsonPath("$.role").value("AGENTE"))
                .andExpect(jsonPath("$.prefeiturasAtribuidasIds[0]").value(prefId.toString()));
    }
}
