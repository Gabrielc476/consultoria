package br.com.govflow.core.infrastructure.adapter.in.rest;

import br.com.govflow.core.application.port.in.AtualizarPrefeituraUseCase;
import br.com.govflow.core.application.port.in.CadastrarPrefeituraUseCase;
import br.com.govflow.core.application.port.in.ConsultarPrefeituraUseCase;
import br.com.govflow.core.domain.exception.CnpjInvalidoException;
import br.com.govflow.core.domain.exception.PrefeituraNaoEncontradaException;
import br.com.govflow.core.domain.model.Cnpj;
import br.com.govflow.core.domain.model.CodigoIbge;
import br.com.govflow.core.domain.model.PorteMunicipio;
import br.com.govflow.core.domain.model.Prefeitura;
import br.com.govflow.core.domain.model.Uf;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.request.CadastrarPrefeituraRequest;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PrefeituraControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CadastrarPrefeituraUseCase cadastrarPrefeituraUseCase;

    @MockBean
    private ConsultarPrefeituraUseCase consultarPrefeituraUseCase;

    @MockBean
    private AtualizarPrefeituraUseCase atualizarPrefeituraUseCase;

    private final UUID tenantId = UUID.randomUUID();

    @Test
    @DisplayName("Deve rejeitar requisição sem header X-Tenant-Id retornando HTTP 400 Problem Details")
    void deveRejeitarRequisicaoSemHeaderTenantId() throws Exception {
        CadastrarPrefeituraRequest request = new CadastrarPrefeituraRequest(
                "08.778.326/0001-56",
                "Prefeitura Municipal de João Pessoa",
                "João Pessoa",
                "PB",
                "2507507",
                PorteMunicipio.GRANDE_PORTE,
                "Cícero Lucena",
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/prefeituras")
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
        CadastrarPrefeituraRequest request = new CadastrarPrefeituraRequest(
                "08.778.326/0001-56",
                "Prefeitura Municipal de João Pessoa",
                "João Pessoa",
                "PB",
                "2507507",
                PorteMunicipio.GRANDE_PORTE,
                "Cícero Lucena",
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/prefeituras")
                        .header("X-Tenant-Id", "not-a-valid-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.title").value("Header de Tenant Inválido"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Deve cadastrar prefeitura com sucesso retornando HTTP 201 e header Location")
    void deveCadastrarPrefeituraComSucesso() throws Exception {
        CadastrarPrefeituraRequest request = new CadastrarPrefeituraRequest(
                "08.778.326/0001-56",
                "Prefeitura Municipal de João Pessoa",
                "João Pessoa",
                "PB",
                "2507507",
                PorteMunicipio.GRANDE_PORTE,
                "Cícero Lucena",
                null,
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2028, 12, 31)
        );

        Prefeitura prefeitura = Prefeitura.criarNova(
                tenantId,
                new Cnpj("08.778.326/0001-56"),
                "Prefeitura Municipal de João Pessoa",
                "João Pessoa",
                Uf.PB,
                new CodigoIbge("2507507"),
                PorteMunicipio.GRANDE_PORTE,
                "Cícero Lucena",
                null,
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2028, 12, 31)
        );

        when(cadastrarPrefeituraUseCase.cadastrar(any())).thenReturn(prefeitura);

        mockMvc.perform(post("/api/v1/prefeituras")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(prefeitura.getId().toString()))
                .andExpect(jsonPath("$.razaoSocial").value("Prefeitura Municipal de João Pessoa"))
                .andExpect(jsonPath("$.cnpj").value("08.778.326/0001-56"));
    }

    @Test
    @DisplayName("Deve retornar HTTP 422 quando violação de domínio de CNPJ ocorrer")
    void deveRetornar422QuandoViolacaoDeDominioOcorrer() throws Exception {
        CadastrarPrefeituraRequest request = new CadastrarPrefeituraRequest(
                "08.778.326/0001-56",
                "Prefeitura Teste",
                "Teste",
                "PB",
                "2507507",
                PorteMunicipio.PEQUENO_PORTE_1,
                null,
                null,
                null,
                null
        );

        when(cadastrarPrefeituraUseCase.cadastrar(any()))
                .thenThrow(new CnpjInvalidoException("Dígitos verificadores do CNPJ são inválidos."));

        mockMvc.perform(post("/api/v1/prefeituras")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.title").value("Regra de Domínio Violada"))
                .andExpect(jsonPath("$.errorCode").value("CNPJ_INVALIDO"));
    }

    @Test
    @DisplayName("Deve retornar HTTP 404 Problem Details quando prefeitura não for encontrada")
    void deveRetornar404QuandoNaoEncontrada() throws Exception {
        UUID idInexistente = UUID.randomUUID();
        when(consultarPrefeituraUseCase.buscarPorId(idInexistente)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/prefeituras/{id}", idInexistente)
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.title").value("Recurso Não Encontrado"));
    }

    @Test
    @DisplayName("Deve listar prefeituras retornando HTTP 200 e envelope PageResponse")
    void deveListarPrefeiturasPaginado() throws Exception {
        Prefeitura prefeitura = Prefeitura.criarNova(
                tenantId,
                new Cnpj("08.778.326/0001-56"),
                "Prefeitura Municipal de João Pessoa",
                "João Pessoa",
                Uf.PB,
                new CodigoIbge("2507507"),
                PorteMunicipio.GRANDE_PORTE,
                "Cícero",
                null,
                null,
                null
        );

        when(consultarPrefeituraUseCase.listar(0, 20, null)).thenReturn(List.of(prefeitura));
        when(consultarPrefeituraUseCase.contar(null)).thenReturn(1L);

        mockMvc.perform(get("/api/v1/prefeituras")
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(prefeitura.getId().toString()))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.pageNumber").value(0));
    }
}
