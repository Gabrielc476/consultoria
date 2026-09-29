package br.com.govflow.core.infrastructure.adapter.in.rest;

import br.com.govflow.core.application.port.in.CadastrarConvenioUseCase;
import br.com.govflow.core.application.port.in.ConsultarConveniosUseCase;
import br.com.govflow.core.domain.exception.ConvenioNaoEncontradoException;
import br.com.govflow.core.domain.model.convenio.Convenio;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.request.CadastrarConvenioRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConvenioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CadastrarConvenioUseCase cadastrarConvenioUseCase;

    @MockBean
    private ConsultarConveniosUseCase consultarConveniosUseCase;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID prefeituraId = UUID.randomUUID();

    @Test
    @DisplayName("Deve rejeitar requisição sem header X-Tenant-Id retornando HTTP 400 Problem Details")
    void deveRejeitarRequisicaoSemHeaderTenantId() throws Exception {
        CadastrarConvenioRequest request = new CadastrarConvenioRequest(
                prefeituraId,
                "954120/2026",
                "00124/2026",
                "Ministério das Cidades",
                "Pavimentação de Vias Urbanas",
                new BigDecimal("1000000.00"),
                new BigDecimal("950000.00"),
                new BigDecimal("50000.00"),
                true,
                LocalDate.now().plusDays(180),
                LocalDate.now(),
                LocalDate.now().plusYears(1)
        );

        mockMvc.perform(post("/api/v1/convenios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.title").value("Header de Tenant Ausente"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Deve rejeitar requisição com header X-Tenant-Id inválido")
    void deveRejeitarRequisicaoComHeaderTenantIdInvalido() throws Exception {
        CadastrarConvenioRequest request = new CadastrarConvenioRequest(
                prefeituraId,
                "954120/2026",
                "00124/2026",
                "Ministério das Cidades",
                "Pavimentação de Vias Urbanas",
                new BigDecimal("1000000.00"),
                new BigDecimal("950000.00"),
                new BigDecimal("50000.00"),
                true,
                LocalDate.now().plusDays(180),
                LocalDate.now(),
                LocalDate.now().plusYears(1)
        );

        mockMvc.perform(post("/api/v1/convenios")
                        .header("X-Tenant-Id", "uuid-invalido")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.title").value("Header de Tenant Inválido"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Deve cadastrar convênio com sucesso retornando HTTP 201 e header Location")
    void deveCadastrarConvenioComSucesso() throws Exception {
        CadastrarConvenioRequest request = new CadastrarConvenioRequest(
                prefeituraId,
                "954120/2026",
                "00124/2026",
                "Ministério das Cidades",
                "Pavimentação de Vias Urbanas",
                new BigDecimal("1000000.00"),
                new BigDecimal("950000.00"),
                new BigDecimal("50000.00"),
                true,
                LocalDate.now().plusDays(180),
                LocalDate.now(),
                LocalDate.now().plusYears(1)
        );

        Convenio convenio = Convenio.criarNovo(
                tenantId,
                prefeituraId,
                "954120/2026",
                "00124/2026",
                "Ministério das Cidades",
                "Pavimentação de Vias Urbanas",
                new BigDecimal("1000000.00"),
                new BigDecimal("950000.00"),
                new BigDecimal("50000.00"),
                true,
                LocalDate.now().plusDays(180),
                LocalDate.now(),
                LocalDate.now().plusYears(1)
        );

        when(cadastrarConvenioUseCase.cadastrar(any())).thenReturn(convenio);

        mockMvc.perform(post("/api/v1/convenios")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(convenio.getId().toString()))
                .andExpect(jsonPath("$.numeroSiconv").value("954120/2026"))
                .andExpect(jsonPath("$.objeto").value("Pavimentação de Vias Urbanas"))
                .andExpect(jsonPath("$.statusClausulaSuspensiva").value("PENDENTE"));
    }

    @Test
    @DisplayName("Deve rejeitar convênio com formato inválido de SICONV retornando 400")
    void deveRejeitarConvenioComSiconvInvalido() throws Exception {
        CadastrarConvenioRequest request = new CadastrarConvenioRequest(
                prefeituraId,
                "formato-invalido",
                "00124/2026",
                "Ministério das Cidades",
                "Pavimentação de Vias Urbanas",
                new BigDecimal("1000000.00"),
                new BigDecimal("950000.00"),
                new BigDecimal("50000.00"),
                false,
                null,
                LocalDate.now(),
                LocalDate.now().plusYears(1)
        );

        mockMvc.perform(post("/api/v1/convenios")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.title").value("Erro de Validação"));
    }

    @Test
    @DisplayName("Deve buscar convênio por ID retornando HTTP 200")
    void deveBuscarConvenioPorId() throws Exception {
        Convenio convenio = Convenio.criarNovo(
                tenantId,
                prefeituraId,
                "954120/2026",
                "00124/2026",
                "Ministério das Cidades",
                "Pavimentação de Vias Urbanas",
                new BigDecimal("1000000.00"),
                new BigDecimal("950000.00"),
                new BigDecimal("50000.00"),
                false,
                null,
                LocalDate.now(),
                LocalDate.now().plusYears(1)
        );

        when(consultarConveniosUseCase.buscarPorId(convenio.getId())).thenReturn(Optional.of(convenio));

        mockMvc.perform(get("/api/v1/convenios/{id}", convenio.getId())
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(convenio.getId().toString()))
                .andExpect(jsonPath("$.numeroSiconv").value("954120/2026"));
    }

    @Test
    @DisplayName("Deve retornar HTTP 404 quando convênio não for encontrado por ID")
    void deveRetornar404QuandoConvenioNaoEncontrado() throws Exception {
        UUID idInexistente = UUID.randomUUID();
        when(consultarConveniosUseCase.buscarPorId(idInexistente)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/convenios/{id}", idInexistente)
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso Não Encontrado"));
    }

    @Test
    @DisplayName("Deve listar convênios por prefeitura retornando HTTP 200")
    void deveListarConveniosPorPrefeitura() throws Exception {
        Convenio convenio = Convenio.criarNovo(
                tenantId,
                prefeituraId,
                "954120/2026",
                "00124/2026",
                "Ministério das Cidades",
                "Pavimentação de Vias Urbanas",
                new BigDecimal("1000000.00"),
                new BigDecimal("950000.00"),
                new BigDecimal("50000.00"),
                false,
                null,
                LocalDate.now(),
                LocalDate.now().plusYears(1)
        );

        when(consultarConveniosUseCase.listarPorPrefeitura(prefeituraId)).thenReturn(List.of(convenio));

        mockMvc.perform(get("/api/v1/convenios")
                        .param("prefeituraId", prefeituraId.toString())
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(convenio.getId().toString()))
                .andExpect(jsonPath("$[0].numeroSiconv").value("954120/2026"));
    }
}
