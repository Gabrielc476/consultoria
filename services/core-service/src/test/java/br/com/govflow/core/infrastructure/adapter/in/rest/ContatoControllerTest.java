package br.com.govflow.core.infrastructure.adapter.in.rest;

import br.com.govflow.core.application.port.in.TriagemUseCase;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.request.CadastrarContatoRequest;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.response.ContatoResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ContatoController.class)
@AutoConfigureMockMvc(addFilters = false)
class ContatoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TriagemUseCase triagemUseCase;

    private final UUID tenantId = UUID.randomUUID();

    @Test
    @DisplayName("GET /api/v1/contatos deve retornar lista de contatos com status 200")
    void deveRetornarListaDeContatos() throws Exception {
        UUID id = UUID.randomUUID();
        ContatoResponse contato = new ContatoResponse(
                id,
                tenantId,
                "5583999998888",
                "Maria Engenheira",
                "FISCAL_ENGENHEIRO",
                "Prefeitura Municipal",
                true,
                Instant.now(),
                Collections.emptyList()
        );

        when(triagemUseCase.listarContatos()).thenReturn(List.of(contato));

        mockMvc.perform(get("/api/v1/contatos")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].nome").value("Maria Engenheira"));
    }

    @Test
    @DisplayName("POST /api/v1/contatos deve criar contato e retornar 201")
    void deveCadastrarContato() throws Exception {
        CadastrarContatoRequest request = new CadastrarContatoRequest(
                "João Fiscal",
                "5583988887777",
                "FISCAL",
                "Secretaria de Obras",
                List.of(UUID.randomUUID()),
                null
        );

        UUID id = UUID.randomUUID();
        ContatoResponse response = new ContatoResponse(
                id,
                tenantId,
                "5583988887777",
                "João Fiscal",
                "FISCAL",
                "Secretaria de Obras",
                true,
                Instant.now(),
                Collections.emptyList()
        );

        when(triagemUseCase.cadastrarContato(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/contatos")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.nome").value("João Fiscal"));
    }
}
