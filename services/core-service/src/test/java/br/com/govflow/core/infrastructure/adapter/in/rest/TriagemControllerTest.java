package br.com.govflow.core.infrastructure.adapter.in.rest;

import br.com.govflow.core.application.port.in.TriagemUseCase;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.request.CadastrarContatoETriarRequest;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.response.TriagemItemResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TriagemController.class)
@AutoConfigureMockMvc(addFilters = false)
class TriagemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TriagemUseCase triagemUseCase;

    private final UUID tenantId = UUID.randomUUID();

    @Test
    @DisplayName("GET /api/v1/triagem/pendentes deve retornar lista de pendentes com status 200")
    void deveRetornarListaDePendentes() throws Exception {
        UUID id = UUID.randomUUID();
        TriagemItemResponse item = new TriagemItemResponse(
                id,
                tenantId,
                null,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "900001/2024",
                "Pavimentação Asfáltica",
                UUID.randomUUID(),
                "04_EXECUCAO_FISICA_E_MEDICOES",
                new BigDecimal("0.75"),
                "Ambiguidade de Convênio",
                "5583999998888",
                "Carlos Silva",
                "Carlos",
                false,
                "Recebido via WhatsApp",
                "PENDENTE",
                null,
                Instant.now(),
                "medicao_01.pdf",
                "application/pdf",
                1024L
        );

        when(triagemUseCase.listarPendentes()).thenReturn(List.of(item));

        mockMvc.perform(get("/api/v1/triagem/pendentes")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].convenioSugeridoNumeroSiconv").value("900001/2024"))
                .andExpect(jsonPath("$[0].status").value("PENDENTE"));
    }

    @Test
    @DisplayName("POST /api/v1/triagem/{inboxId}/cadastrar-contato-e-arquivar deve processar e retornar 200")
    void deveCadastrarContatoEArquivarComSucesso() throws Exception {
        UUID inboxId = UUID.randomUUID();
        UUID convId = UUID.randomUUID();

        CadastrarContatoETriarRequest request = new CadastrarContatoETriarRequest(
                "Eng. Carlos",
                "5583999991234",
                "FISCAL_ENGENHEIRO",
                "Empresa XPTO",
                List.of(convId),
                convId,
                "04_EXECUCAO_FISICA_E_MEDICOES",
                true
        );

        TriagemItemResponse response = new TriagemItemResponse(
                inboxId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                convId,
                "900001/2024",
                "Obra Central",
                UUID.randomUUID(),
                "04_EXECUCAO_FISICA_E_MEDICOES",
                new BigDecimal("0.95"),
                null,
                "5583999991234",
                "Eng. Carlos",
                "Carlos",
                false,
                "Resumo",
                "RESOLVIDO",
                Instant.now(),
                Instant.now(),
                "doc.pdf",
                "application/pdf",
                100L
        );

        when(triagemUseCase.cadastrarContatoETriar(eq(inboxId), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/triagem/{inboxId}/cadastrar-contato-e-arquivar", inboxId)
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(inboxId.toString()))
                .andExpect(jsonPath("$.status").value("RESOLVIDO"));
    }

    @Test
    @DisplayName("POST /api/v1/triagem/{inboxId}/confirmar-arquivamento deve confirmar em 1 clique")
    void deveConfirmarArquivamentoEm1Clique() throws Exception {
        UUID inboxId = UUID.randomUUID();
        TriagemItemResponse response = new TriagemItemResponse(
                inboxId,
                tenantId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "900001/2024",
                "Objeto",
                UUID.randomUUID(),
                "04_EXECUCAO_FISICA_E_MEDICOES",
                BigDecimal.ONE,
                null,
                "5583999991111",
                "Nome",
                "Push",
                false,
                "Resumo",
                "RESOLVIDO",
                Instant.now(),
                Instant.now(),
                "doc.pdf",
                "application/pdf",
                100L
        );

        when(triagemUseCase.confirmarArquivamento(eq(inboxId), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/triagem/{inboxId}/confirmar-arquivamento", inboxId)
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVIDO"));
    }
}
