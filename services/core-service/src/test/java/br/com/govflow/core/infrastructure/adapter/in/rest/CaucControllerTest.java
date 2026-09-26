package br.com.govflow.core.infrastructure.adapter.in.rest;

import br.com.govflow.core.application.port.in.AtualizarCertidaoCaucUseCase;
import br.com.govflow.core.application.port.in.AvaliarConformidadeCaucUseCase;
import br.com.govflow.core.application.port.in.ConsultarCaucUseCase;
import br.com.govflow.core.application.port.in.ConsultarCaucUseCase.*;
import br.com.govflow.core.domain.model.CertidaoCauc;
import br.com.govflow.core.domain.model.GrupoCauc;
import br.com.govflow.core.domain.model.StatusCauc;
import br.com.govflow.core.domain.model.StatusCertidao;
import br.com.govflow.core.domain.model.TipoExigenciaCauc;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.request.CadastrarCertidaoCaucRequest;
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

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CaucControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ConsultarCaucUseCase consultarCaucUseCase;

    @MockBean
    private AtualizarCertidaoCaucUseCase atualizarCertidaoCaucUseCase;

    @MockBean
    private AvaliarConformidadeCaucUseCase avaliarConformidadeCaucUseCase;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID prefeituraId = UUID.randomUUID();

    @Test
    @DisplayName("Deve rejeitar requisição sem header X-Tenant-Id retornando HTTP 400 Problem Details")
    void deveRejeitarRequisicaoSemHeaderTenantId() throws Exception {
        mockMvc.perform(get("/api/v1/cauc/resumo"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.title").value("Header de Tenant Ausente"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Deve obter dossiê CAUC da prefeitura com sucesso (HTTP 200)")
    void deveObterDossiePrefeituraComSucesso() throws Exception {
        ItemCertidaoDto item = new ItemCertidaoDto(
                UUID.randomUUID(),
                "1.1",
                GrupoCauc.TRIBUTOS_FGTS,
                TipoExigenciaCauc.RECEITA_FEDERAL_PGFN,
                "Certidão Conjunta PGFN / RFB",
                "RFB / PGFN",
                "CND-001",
                LocalDate.now().minusDays(30),
                LocalDate.now().plusDays(60),
                StatusCertidao.REGULAR,
                60,
                null,
                Instant.now()
        );

        DossieCaucDto dossie = new DossieCaucDto(
                prefeituraId,
                "Patos",
                "PB",
                "09.288.665/0001-38",
                StatusCauc.ADIMPLENTE,
                16,
                0,
                0,
                List.of(item)
        );

        when(consultarCaucUseCase.obterDossiePrefeitura(prefeituraId)).thenReturn(dossie);

        mockMvc.perform(get("/api/v1/cauc/prefeituras/" + prefeituraId)
                        .header("X-Tenant-Id", tenantId.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prefeituraId").value(prefeituraId.toString()))
                .andExpect(jsonPath("$.nomeMunicipio").value("Patos"))
                .andExpect(jsonPath("$.statusGeral").value("ADIMPLENTE"))
                .andExpect(jsonPath("$.certidoesRegulares").value(16))
                .andExpect(jsonPath("$.certidoes[0].codigo").value("1.1"))
                .andExpect(jsonPath("$.certidoes[0].status").value("REGULAR"));
    }

    @Test
    @DisplayName("Deve obter resumo geral do Radar CAUC com sucesso (HTTP 200)")
    void deveObterResumoGeralComSucesso() throws Exception {
        ResumoCaucDto resumo = new ResumoCaucDto(
                5,
                75,
                3,
                2,
                List.of()
        );

        when(consultarCaucUseCase.obterResumoConsultoria(tenantId)).thenReturn(resumo);

        mockMvc.perform(get("/api/v1/cauc/resumo")
                        .header("X-Tenant-Id", tenantId.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMunicipios").value(5))
                .andExpect(jsonPath("$.totalRegulares").value(75))
                .andExpect(jsonPath("$.totalAlerta").value(3))
                .andExpect(jsonPath("$.totalVencidas").value(2));
    }

    @Test
    @DisplayName("Deve cadastrar certidão com sucesso retornando HTTP 201 Created")
    void deveCadastrarCertidaoComSucesso() throws Exception {
        CadastrarCertidaoCaucRequest request = new CadastrarCertidaoCaucRequest(
                TipoExigenciaCauc.REGULARIDADE_FGTS,
                "CRF-2026-999",
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                "s3://govflow/crf.pdf",
                null
        );

        CertidaoCauc certidaoSalva = new CertidaoCauc(
                UUID.randomUUID(),
                tenantId,
                prefeituraId,
                TipoExigenciaCauc.REGULARIDADE_FGTS,
                "CRF-2026-999",
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                StatusCertidao.REGULAR,
                30,
                "s3://govflow/crf.pdf",
                Instant.now(),
                Instant.now()
        );

        when(atualizarCertidaoCaucUseCase.cadastrarOuAtualizarCertidao(any())).thenReturn(certidaoSalva);

        mockMvc.perform(post("/api/v1/cauc/prefeituras/" + prefeituraId + "/certidoes")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.tipoExigencia").value(TipoExigenciaCauc.REGULARIDADE_FGTS.name()))
                .andExpect(jsonPath("$.codigo").value("1.2"))
                .andExpect(jsonPath("$.status").value("REGULAR"));
    }

    @Test
    @DisplayName("Deve reavaliar conformidade sob demanda retornando HTTP 200")
    void deveReavaliarConformidadeSobDemanda() throws Exception {
        AvaliarConformidadeCaucUseCase.ResultadoAvaliacaoDto resultado =
                new AvaliarConformidadeCaucUseCase.ResultadoAvaliacaoDto(5, 80, 2, 1, 3, 1);

        when(avaliarConformidadeCaucUseCase.avaliarTodasPrefeituras(tenantId)).thenReturn(resultado);

        mockMvc.perform(post("/api/v1/cauc/avaliar")
                        .header("X-Tenant-Id", tenantId.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPrefeiturasAvaliadas").value(5))
                .andExpect(jsonPath("$.totalCertidoesAvaliadas").value(80))
                .andExpect(jsonPath("$.totalCertidoesEmAlerta").value(2))
                .andExpect(jsonPath("$.totalCertidoesVencidas").value(1))
                .andExpect(jsonPath("$.totalAlertasDisparados").value(3))
                .andExpect(jsonPath("$.prefeiturasBloqueadas").value(1));
    }
}
