package br.com.govflow.core.infrastructure.adapter.in.rest;

import br.com.govflow.core.application.port.in.GerenciarAgenteUseCase;
import br.com.govflow.core.domain.exception.AcessoNegadoException;
import br.com.govflow.core.domain.model.usuario.RoleUsuario;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.request.AtualizarAgenteRequest;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.request.CriarAgenteRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AgenteController.class)
class AgenteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GerenciarAgenteUseCase gerenciarAgenteUseCase;

    private final UUID tenantId = UUID.randomUUID();

    @Test
    @DisplayName("Deve cadastrar agente com sucesso retornando 201 Created")
    void deveCadastrarAgenteComSucesso() throws Exception {
        UUID agenteId = UUID.randomUUID();
        UUID prefId = UUID.randomUUID();

        CriarAgenteRequest request = new CriarAgenteRequest(
                "João Analista",
                "joao@planejabrasil.com.br",
                "GovFlow2026!",
                "+5583988881111",
                Set.of(prefId)
        );

        var response = new GerenciarAgenteUseCase.AgenteResponse(
                agenteId,
                tenantId,
                "João Analista",
                "joao@planejabrasil.com.br",
                "+5583988881111",
                RoleUsuario.AGENTE,
                true,
                Set.of(prefId),
                Instant.now(),
                Instant.now()
        );

        when(gerenciarAgenteUseCase.criar(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/agentes")
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Roles", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(agenteId.toString()))
                .andExpect(jsonPath("$.nome").value("João Analista"))
                .andExpect(jsonPath("$.email").value("joao@planejabrasil.com.br"))
                .andExpect(jsonPath("$.telefoneCelular").value("+5583988881111"))
                .andExpect(jsonPath("$.role").value("AGENTE"))
                .andExpect(jsonPath("$.prefeiturasAtribuidasIds[0]").value(prefId.toString()));
    }

    @Test
    @DisplayName("Deve listar agentes da consultoria retornando 200 OK")
    void deveListarAgentes() throws Exception {
        UUID agenteId = UUID.randomUUID();
        UUID prefId = UUID.randomUUID();

        var response = new GerenciarAgenteUseCase.AgenteResponse(
                agenteId,
                tenantId,
                "João Analista",
                "joao@planejabrasil.com.br",
                "+5583988881111",
                RoleUsuario.AGENTE,
                true,
                Set.of(prefId),
                Instant.now(),
                Instant.now()
        );

        when(gerenciarAgenteUseCase.listar()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/agentes")
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Roles", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(agenteId.toString()))
                .andExpect(jsonPath("$[0].nome").value("João Analista"));
    }

    @Test
    @DisplayName("Deve inativar agente retornando 204 No Content")
    void deveInativarAgente() throws Exception {
        UUID agenteId = UUID.randomUUID();

        doNothing().when(gerenciarAgenteUseCase).inativar(eq(agenteId));

        mockMvc.perform(delete("/api/v1/agentes/" + agenteId)
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Roles", "ADMIN"))
                .andExpect(status().isNoContent());

        verify(gerenciarAgenteUseCase, times(1)).inativar(eq(agenteId));
    }
}
