package br.com.govflow.core.infrastructure.adapter.in.rest;

import br.com.govflow.core.application.port.in.*;
import br.com.govflow.core.application.port.in.ConsultarClausulaSuspensivaUseCase.DossieClausulaSuspensivaDto;
import br.com.govflow.core.application.port.in.ConsultarClausulaSuspensivaUseCase.ItemCondicionanteDto;
import br.com.govflow.core.domain.exception.ClausulaSuspensivaNaoPodeSerSuperadaException;
import br.com.govflow.core.domain.exception.ConvenioNaoEncontradoException;
import br.com.govflow.core.domain.model.convenio.CondicionanteSuspensiva;
import br.com.govflow.core.domain.model.convenio.CriticidadePrazoSuspensiva;
import br.com.govflow.core.domain.model.convenio.StatusCondicionanteSuspensiva;
import br.com.govflow.core.domain.model.convenio.TipoCondicionanteSuspensiva;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.request.AprovarCondicionanteRequest;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.request.RegistrarDiligenciaRequest;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.request.SolicitarProrrogacaoRequest;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.request.SuperarClausulaSuspensivaRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClausulaSuspensivaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ConsultarClausulaSuspensivaUseCase consultarUseCase;

    @MockBean
    private GerenciarCondicionanteUseCase gerenciarUseCase;

    @MockBean
    private ProrrogarPrazoClausulaSuspensivaUseCase prorrogarUseCase;

    @MockBean
    private SuperarClausulaSuspensivaUseCase superarUseCase;

    @MockBean
    private UploadDocumentoCondicionanteUseCase uploadUseCase;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID convenioId = UUID.randomUUID();

    private DossieClausulaSuspensivaDto criarDossieMock() {
        ItemCondicionanteDto pilar1 = new ItemCondicionanteDto(
                UUID.randomUUID(),
                TipoCondicionanteSuspensiva.ENGENHARIA_PROJETOS_SINAPI,
                "Engenharia, Projetos & Orçamento SINAPI",
                StatusCondicionanteSuspensiva.EM_ANALISE_CAIXA,
                "Em Análise pela Caixa",
                "SPA-914250/2026",
                null,
                null,
                "Aguardando vistoria GIGOV",
                "s3/projeto.pdf",
                null,
                null,
                new BigDecimal("2050000.00"),
                new BigDecimal("22.12"),
                "ART-PB-12345",
                "Caixa GIGOV"
        );

        return new DossieClausulaSuspensivaDto(
                convenioId,
                UUID.randomUUID(),
                "914250/2023",
                "00124/2023",
                "FNDE / MEC",
                "Construção de Creche Proinfância Tipo 2",
                new BigDecimal("2050000.00"),
                new BigDecimal("1850000.00"),
                new BigDecimal("200000.00"),
                true,
                LocalDate.now().plusDays(120),
                false,
                null,
                LocalDate.now().plusDays(120),
                120,
                CriticidadePrazoSuspensiva.REGULAR,
                false,
                null,
                List.of(pilar1)
        );
    }

    @Test
    @DisplayName("Deve rejeitar requisição sem header X-Tenant-Id retornando HTTP 400 Problem Details")
    void deveRejeitarRequisicaoSemHeaderTenantId() throws Exception {
        mockMvc.perform(get("/api/v1/convenios/" + convenioId + "/clausula-suspensiva"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.title").value("Header de Tenant Ausente"));
    }

    @Test
    @DisplayName("Deve obter dossiê da cláusula suspensiva por ID do convênio (HTTP 200)")
    void deveObterDossiePorConvenioId() throws Exception {
        DossieClausulaSuspensivaDto dossie = criarDossieMock();
        when(consultarUseCase.obterDossiePorConvenioId(convenioId)).thenReturn(dossie);

        mockMvc.perform(get("/api/v1/convenios/" + convenioId + "/clausula-suspensiva")
                        .header("X-Tenant-Id", tenantId.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.convenioId").value(convenioId.toString()))
                .andExpect(jsonPath("$.numeroSiconv").value("914250/2023"))
                .andExpect(jsonPath("$.diasRestantes").value(120))
                .andExpect(jsonPath("$.criticidade").value("REGULAR"))
                .andExpect(jsonPath("$.superada").value(false))
                .andExpect(jsonPath("$.condicionantes[0].tipo").value("ENGENHARIA_PROJETOS_SINAPI"));
    }

    @Test
    @DisplayName("Deve submeter condicionante para auditoria técnica da Caixa (HTTP 200)")
    void deveSubmeterCondicionanteParaAnaliseCaixa() throws Exception {
        var cond = CondicionanteSuspensiva.nova(tenantId, convenioId, TipoCondicionanteSuspensiva.ENGENHARIA_PROJETOS_SINAPI);
        cond.submeterParaAnaliseCaixa();
        when(gerenciarUseCase.submeterParaAnaliseCaixa(convenioId, TipoCondicionanteSuspensiva.ENGENHARIA_PROJETOS_SINAPI))
                .thenReturn(cond);

        mockMvc.perform(post("/api/v1/convenios/" + convenioId + "/clausula-suspensiva/condicionantes/ENGENHARIA_PROJETOS_SINAPI/submeter")
                        .header("X-Tenant-Id", tenantId.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("ENGENHARIA_PROJETOS_SINAPI"))
                .andExpect(jsonPath("$.status").value("EM_ANALISE_CAIXA"));
    }

    @Test
    @DisplayName("Deve registrar diligência da Caixa com laudo de pendências (HTTP 200)")
    void deveRegistrarDiligencia() throws Exception {
        var cond = CondicionanteSuspensiva.nova(tenantId, convenioId, TipoCondicionanteSuspensiva.LICENCIAMENTO_AMBIENTAL);
        LocalDate prazoSaneamento = LocalDate.now().plusDays(25);
        cond.registrarDiligencia("Adequar Licença de Instalação", "s3/laudo_amb.pdf", prazoSaneamento, LocalDate.now());

        when(gerenciarUseCase.registrarDiligenciaCaixa(any())).thenReturn(cond);

        var request = new RegistrarDiligenciaRequest(
                "Adequar Licença de Instalação",
                "s3/laudo_amb.pdf",
                prazoSaneamento
        );

        mockMvc.perform(post("/api/v1/convenios/" + convenioId + "/clausula-suspensiva/condicionantes/LICENCIAMENTO_AMBIENTAL/diligencia")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DILIGENCIA_EMITIDA"))
                .andExpect(jsonPath("$.observacoesAnaliseCaixa").value("Adequar Licença de Instalação"));
    }

    @Test
    @DisplayName("Deve aprovar condicionante técnica da Caixa (HTTP 200)")
    void deveAprovarCondicionante() throws Exception {
        var cond = CondicionanteSuspensiva.nova(tenantId, convenioId, TipoCondicionanteSuspensiva.ENGENHARIA_PROJETOS_SINAPI);
        cond.aprovar("SPA-914250/2026", LocalDate.now(), null, new BigDecimal("2050000.00"), new BigDecimal("22.50"), "ART-01", "GIGOV", "s3/lae.pdf");

        when(gerenciarUseCase.aprovarCondicionante(any())).thenReturn(cond);

        var request = new AprovarCondicionanteRequest(
                "SPA-914250/2026",
                LocalDate.now(),
                null,
                new BigDecimal("2050000.00"),
                new BigDecimal("22.50"),
                "ART-01",
                "GIGOV",
                "s3/lae.pdf"
        );

        mockMvc.perform(post("/api/v1/convenios/" + convenioId + "/clausula-suspensiva/condicionantes/ENGENHARIA_PROJETOS_SINAPI/aprovar")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APROVADO"))
                .andExpect(jsonPath("$.numeroDocumentoComprobatorio").value("SPA-914250/2026"));
    }

    @Test
    @DisplayName("Deve solicitar prorrogação de prazo fatal (HTTP 200)")
    void deveSolicitarProrrogacao() throws Exception {
        DossieClausulaSuspensivaDto dossie = criarDossieMock();
        when(consultarUseCase.obterDossiePorConvenioId(convenioId)).thenReturn(dossie);

        var request = new SolicitarProrrogacaoRequest(LocalDate.now().plusDays(180));

        mockMvc.perform(post("/api/v1/convenios/" + convenioId + "/clausula-suspensiva/prorrogacao")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.convenioId").value(convenioId.toString()));
    }

    @Test
    @DisplayName("Deve superar Cláusula Suspensiva com sucesso (HTTP 200)")
    void deveSuperarClausulaSuspensiva() throws Exception {
        DossieClausulaSuspensivaDto dossie = criarDossieMock();
        when(consultarUseCase.obterDossiePorConvenioId(convenioId)).thenReturn(dossie);

        var request = new SuperarClausulaSuspensivaRequest("s3/termo_retirada.pdf");

        mockMvc.perform(post("/api/v1/convenios/" + convenioId + "/clausula-suspensiva/superar")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.convenioId").value(convenioId.toString()));
    }

    @Test
    @DisplayName("Deve fazer upload de documento comprobatório via Multipart (HTTP 200)")
    void deveFazerUploadDeDocumento() throws Exception {
        var cond = CondicionanteSuspensiva.nova(tenantId, convenioId, TipoCondicionanteSuspensiva.TITULARIDADE_IMOVEL);
        cond.vincularDocumento("clausula-suspensiva/cri.pdf");
        when(uploadUseCase.uploadDocumentoComprobatorio(eq(convenioId), eq(TipoCondicionanteSuspensiva.TITULARIDADE_IMOVEL), any(), any(), any()))
                .thenReturn(cond);

        MockMultipartFile file = new MockMultipartFile(
                "arquivo",
                "certidao_cri.pdf",
                "application/pdf",
                "conteudo do pdf".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/convenios/" + convenioId + "/clausula-suspensiva/condicionantes/TITULARIDADE_IMOVEL/documentos")
                        .file(file)
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("TITULARIDADE_IMOVEL"))
                .andExpect(jsonPath("$.s3KeyDocumento").value("clausula-suspensiva/cri.pdf"));
    }

    @Test
    @DisplayName("Deve retornar HTTP 404 quando o convênio não for encontrado")
    void deveRetornar404QuandoConvenioNaoEncontrado() throws Exception {
        when(consultarUseCase.obterDossiePorConvenioId(convenioId))
                .thenThrow(new ConvenioNaoEncontradoException(convenioId));

        mockMvc.perform(get("/api/v1/convenios/" + convenioId + "/clausula-suspensiva")
                        .header("X-Tenant-Id", tenantId.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso Não Encontrado"))
                .andExpect(jsonPath("$.errorCode").value("CONVENIO_NAO_ENCONTRADO"));
    }

    @Test
    @DisplayName("Deve retornar HTTP 422 quando superação for bloqueada por pendências nos pilares")
    void deveRetornar422QuandoBloqueadoPorPilaresPendentes() throws Exception {
        when(superarUseCase.superarClausulaSuspensiva(eq(convenioId), any()))
                .thenThrow(new ClausulaSuspensivaNaoPodeSerSuperadaException(convenioId, List.of(TipoCondicionanteSuspensiva.LICENCIAMENTO_AMBIENTAL)));

        var request = new SuperarClausulaSuspensivaRequest("s3/termo.pdf");

        mockMvc.perform(post("/api/v1/convenios/" + convenioId + "/clausula-suspensiva/superar")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Regra de Domínio Violada"))
                .andExpect(jsonPath("$.errorCode").value("CLAUSULA_SUSPENSIVA_BLOQUEADA"));
    }
}
