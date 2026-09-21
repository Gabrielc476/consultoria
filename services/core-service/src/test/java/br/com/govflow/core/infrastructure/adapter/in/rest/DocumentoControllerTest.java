package br.com.govflow.core.infrastructure.adapter.in.rest;

import br.com.govflow.core.application.port.in.AprovarDocumentoUseCase;
import br.com.govflow.core.application.port.in.ConsultarDocumentoUseCase;
import br.com.govflow.core.application.port.in.RejeitarDocumentoUseCase;
import br.com.govflow.core.domain.model.*;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.request.AprovarDocumentoRequest;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.request.DadosRevisaoRequest;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.request.RejeitarDocumentoRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentoController.class)
@Import(DocumentoRestMapper.class)
class DocumentoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ConsultarDocumentoUseCase consultarUseCase;

    @MockBean
    private AprovarDocumentoUseCase aprovarUseCase;

    @MockBean
    private RejeitarDocumentoUseCase rejeitarUseCase;

    @MockBean
    private br.com.govflow.core.application.port.in.ObterArquivoDocumentoUseCase obterArquivoUseCase;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Deve retornar 400 quando header X-Tenant-Id estiver ausente")
    void deveRetornar400QuandoTenantAusente() throws Exception {
        mockMvc.perform(get("/api/v1/documentos/" + UUID.randomUUID()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Header de Tenant Ausente"));
    }

    @Test
    @DisplayName("Deve buscar documento por ID com sucesso e retornar scores por campo")
    void deveBuscarDocumentoPorId() throws Exception {
        UUID docId = UUID.randomUUID();
        Documento doc = Documento.criarRecebido(
                docId,
                tenantId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "bucket",
                "docs/nf1.pdf",
                "nf1.pdf",
                "application/pdf",
                1024L
        );
        doc.registrarExtracaoIA(
                new ExtracaoSugerida(
                        TipoDocumentoHabil.NOTA_FISCAL_SERVICOS,
                        "000123",
                        "1",
                        null,
                        LocalDate.of(2026, 5, 10),
                        "12.345.678/0001-95",
                        "Empresa Teste",
                        "Serviços",
                        null,
                        new BigDecimal("1000.00"),
                        BigDecimal.ZERO,
                        new BigDecimal("1000.00"),
                        List.of(),
                        0.98,
                        Map.of("valorBruto", 0.99, "cnpjCredor", 0.95),
                        true,
                        List.of()
                ),
                Map.of("numeroDocumento", new BoundingBox(0.1, 0.1, 0.2, 0.2))
        );

        when(consultarUseCase.buscarPorId(docId)).thenReturn(Optional.of(doc));

        mockMvc.perform(get("/api/v1/documentos/" + docId)
                        .header("X-Tenant-Id", tenantId.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(docId.toString()))
                .andExpect(jsonPath("$.status").value("EM_CONFERENCIA"))
                .andExpect(jsonPath("$.extracaoSugerida.numeroDocumento").value("000123"))
                .andExpect(jsonPath("$.extracaoSugerida.scoresConfiancaCampos.valorBruto").value(0.99))
                .andExpect(jsonPath("$.boundingBoxes.numeroDocumento.ymin").value(0.1));
    }

    @Test
    @DisplayName("Deve retornar 404 quando documento não for encontrado")
    void deveRetornar404QuandoNaoEncontrado() throws Exception {
        UUID docId = UUID.randomUUID();
        when(consultarUseCase.buscarPorId(docId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/documentos/" + docId)
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("DOCUMENTO_NAO_ENCONTRADO"));
    }

    @Test
    @DisplayName("Deve listar documentos com paginação")
    void deveListarDocumentosComPaginacao() throws Exception {
        UUID docId = UUID.randomUUID();
        Documento doc = Documento.criarRecebido(docId, tenantId, null, null, "b", "k", "n", "m", 1L);

        when(consultarUseCase.listar(eq(0), eq(10), any())).thenReturn(List.of(doc));
        when(consultarUseCase.contar(any())).thenReturn(1L);

        mockMvc.perform(get("/api/v1/documentos?page=0&size=10")
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(docId.toString()));
    }

    @Test
    @DisplayName("Deve aprovar documento via PUT retornando documento atualizado")
    void deveAprovarDocumento() throws Exception {
        UUID docId = UUID.randomUUID();
        UUID analistaId = UUID.randomUUID();

        Documento doc = Documento.criarRecebido(docId, tenantId, null, null, "b", "k", "n", "m", 1L);
        doc.registrarExtracaoIA(
                new ExtracaoSugerida(
                        TipoDocumentoHabil.NOTA_FISCAL_SERVICOS,
                        "000123",
                        "1",
                        null,
                        LocalDate.of(2026, 5, 10),
                        "12.345.678/0001-95",
                        "Empresa Teste",
                        "Serviços",
                        null,
                        new BigDecimal("1000.00"),
                        BigDecimal.ZERO,
                        new BigDecimal("1000.00"),
                        List.of(),
                        0.98,
                        true,
                        List.of()
                ),
                Map.of()
        );

        DadosRevisaoAnalista rev = new DadosRevisaoAnalista(
                TipoDocumentoHabil.NOTA_FISCAL_SERVICOS,
                "000123",
                "1",
                null,
                LocalDate.of(2026, 5, 10),
                "12.345.678/0001-95",
                "Empresa Teste",
                "Serviços",
                null,
                new BigDecimal("1000.00"),
                BigDecimal.ZERO,
                new BigDecimal("1000.00"),
                List.of(),
                "Aprovado"
        );

        AuditoriaRevisao auditoria = doc.aprovar(analistaId, rev, "OK");

        when(aprovarUseCase.aprovar(any())).thenReturn(new AprovarDocumentoUseCase.ResultadoAprovacao(doc, auditoria));

        DadosRevisaoRequest reqRevisao = new DadosRevisaoRequest(
                "NOTA_FISCAL_SERVICOS",
                "000123",
                "1",
                null,
                LocalDate.of(2026, 5, 10),
                "12.345.678/0001-95",
                "Empresa Teste",
                "Serviços",
                null,
                new BigDecimal("1000.00"),
                BigDecimal.ZERO,
                new BigDecimal("1000.00"),
                List.of(),
                "Aprovado"
        );
        AprovarDocumentoRequest request = new AprovarDocumentoRequest(analistaId, reqRevisao, "OK");

        mockMvc.perform(put("/api/v1/documentos/" + docId + "/aprovar")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PRONTO_PARA_TRANSFEREGOV"));
    }

    @Test
    @DisplayName("Deve rejeitar aprovação com HTTP 400 se analistaId for omitido")
    void deveRejeitarAprovacaoSemAnalistaId() throws Exception {
        UUID docId = UUID.randomUUID();

        DadosRevisaoRequest reqRevisao = new DadosRevisaoRequest(
                "NOTA_FISCAL_SERVICOS",
                "000123",
                "1",
                null,
                LocalDate.of(2026, 5, 10),
                "12.345.678/0001-95",
                "Empresa Teste",
                "Serviços",
                null,
                new BigDecimal("1000.00"),
                BigDecimal.ZERO,
                new BigDecimal("1000.00"),
                List.of(),
                "Aprovado"
        );
        AprovarDocumentoRequest request = new AprovarDocumentoRequest(null, reqRevisao, "OK");

        mockMvc.perform(put("/api/v1/documentos/" + docId + "/aprovar")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve rejeitar documento via PUT retornando documento rejeitado")
    void deveRejeitarDocumento() throws Exception {
        UUID docId = UUID.randomUUID();
        UUID analistaId = UUID.randomUUID();

        Documento doc = Documento.criarRecebido(docId, tenantId, null, null, "b", "k", "n", "m", 1L);
        doc.registrarExtracaoIA(
                new ExtracaoSugerida(
                        TipoDocumentoHabil.NOTA_FISCAL_SERVICOS,
                        "000123",
                        "1",
                        null,
                        LocalDate.of(2026, 5, 10),
                        "12.345.678/0001-95",
                        "Empresa Teste",
                        "Serviços",
                        null,
                        new BigDecimal("1000.00"),
                        BigDecimal.ZERO,
                        new BigDecimal("1000.00"),
                        List.of(),
                        0.98,
                        true,
                        List.of()
                ),
                Map.of()
        );

        AuditoriaRevisao auditoria = doc.rejeitar(analistaId, "Documento rasurado");

        when(rejeitarUseCase.rejeitar(any())).thenReturn(new RejeitarDocumentoUseCase.ResultadoRejeicao(doc, auditoria));

        RejeitarDocumentoRequest request = new RejeitarDocumentoRequest(analistaId, "Documento rasurado");

        mockMvc.perform(put("/api/v1/documentos/" + docId + "/rejeitar")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJEITADO"))
                .andExpect(jsonPath("$.motivoRejeicao").value("Documento rasurado"));
    }

    @Test
    @DisplayName("Deve listar histórico de auditoria do documento")
    void deveListarAuditoria() throws Exception {
        UUID docId = UUID.randomUUID();
        AuditoriaRevisao auditoria = AuditoriaRevisao.criarRejeicao(
                tenantId,
                docId,
                UUID.randomUUID(),
                "Nota reprovada",
                Map.of()
        );

        when(consultarUseCase.listarAuditorias(docId)).thenReturn(List.of(auditoria));

        mockMvc.perform(get("/api/v1/documentos/" + docId + "/auditoria")
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].acao").value("REJEICAO"))
                .andExpect(jsonPath("$[0].justificativa").value("Nota reprovada"));
    }

    @Test
    @DisplayName("Deve retornar 422 Unprocessable Entity com detalhes matemáticos estruturados quando ocorrer InconsistenciaMatematicaException")
    void deveRetornar422QuandoHouverInconsistenciaMatematica() throws Exception {
        UUID docId = UUID.randomUUID();
        when(aprovarUseCase.aprovar(any())).thenThrow(
                new br.com.govflow.core.domain.exception.InconsistenciaMatematicaException(
                        new BigDecimal("1000.00"), BigDecimal.ZERO, new BigDecimal("800.00"), new BigDecimal("200.00")
                )
        );

        AprovarDocumentoRequest request = new AprovarDocumentoRequest(
                UUID.randomUUID(),
                new DadosRevisaoRequest(
                        "NOTA_FISCAL_SERVICOS",
                        "123",
                        "1",
                        null,
                        LocalDate.of(2026, 5, 10),
                        "12.345.678/0001-95",
                        "Empresa Teste",
                        "Serviços",
                        null,
                        new BigDecimal("1000.00"),
                        BigDecimal.ZERO,
                        new BigDecimal("800.00"),
                        List.of(),
                        null
                ),
                null
        );

        mockMvc.perform(put("/api/v1/documentos/" + docId + "/aprovar")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("INCONSISTENCIA_MATEMATICA_FISCAL"))
                .andExpect(jsonPath("$.valorBruto").value(1000.00))
                .andExpect(jsonPath("$.diferenca").value(200.00))
                .andExpect(jsonPath("$.tolerancia").value("0.00"));
    }

    @Test
    @DisplayName("Deve retornar streaming de arquivo PDF com sucesso")
    void deveRetornarArquivoPdfComSucesso() throws Exception {
        UUID docId = UUID.randomUUID();
        byte[] conteudo = "%PDF-1.4 teste".getBytes(java.nio.charset.StandardCharsets.UTF_8);

        when(obterArquivoUseCase.obterArquivo(docId)).thenReturn(
                new br.com.govflow.core.application.port.in.ObterArquivoDocumentoUseCase.ArquivoConteudo(
                        new java.io.ByteArrayInputStream(conteudo),
                        "application/pdf",
                        "nota_fiscal.pdf",
                        conteudo.length
                )
        );

        mockMvc.perform(get("/api/v1/documentos/" + docId + "/arquivo")
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Content-Type", "application/pdf"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Content-Disposition", org.hamcrest.Matchers.containsString("nota_fiscal.pdf")));
    }
}
