package br.com.govflow.core.infrastructure.adapter.in.rest;

import br.com.govflow.core.application.port.in.ExportarFicheiroZipUseCase;
import br.com.govflow.core.application.port.in.GerenciarFicheiroDigitalUseCase;
import br.com.govflow.core.domain.model.ArmazenamentoArquivo;
import br.com.govflow.core.domain.model.Documento;
import br.com.govflow.core.domain.model.documento.CategoriaDocumento;
import br.com.govflow.core.domain.model.documento.FaseCicloVida;
import br.com.govflow.core.domain.model.documento.FicheiroDigital;
import br.com.govflow.core.domain.model.documento.OrigemCanal;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.request.ExcluirDocumentoRequest;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.request.MoverDocumentoRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FicheiroDigitalController.class)
@Import(FicheiroRestMapper.class)
class FicheiroDigitalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GerenciarFicheiroDigitalUseCase ficheiroUseCase;

    @MockBean
    private ExportarFicheiroZipUseCase exportarZipUseCase;

    private UUID tenantId;
    private UUID prefeituraId;
    private UUID convenioId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        prefeituraId = UUID.randomUUID();
        convenioId = UUID.randomUUID();
    }

    @Test
    @DisplayName("GET /api/v1/convenios/{convenioId}/ficheiro deve retornar estrutura das 10 Fases")
    void deveObterFicheiroDigital() throws Exception {
        FicheiroDigital.PastaFase pasta1 = new FicheiroDigital.PastaFase(
                FaseCicloVida.FASE_01_CELEBRACAO,
                "01",
                "01_Celebracao_e_Formalizacao",
                "Celebração e Formalização",
                1,
                1024L,
                List.of()
        );
        FicheiroDigital ficheiro = new FicheiroDigital(
                convenioId,
                prefeituraId,
                tenantId,
                "942100/2024",
                "Objeto teste",
                List.of(pasta1)
        );

        when(ficheiroUseCase.obterFicheiro(convenioId)).thenReturn(ficheiro);

        mockMvc.perform(get("/api/v1/convenios/" + convenioId + "/ficheiro")
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.convenioId").value(convenioId.toString()))
                .andExpect(jsonPath("$.numeroSiconv").value("942100/2024"))
                .andExpect(jsonPath("$.totalArquivos").value(1))
                .andExpect(jsonPath("$.fases").isArray())
                .andExpect(jsonPath("$.fases[0].fase").value("FASE_01_CELEBRACAO"));
    }

    @Test
    @DisplayName("POST /api/v1/convenios/{convenioId}/ficheiro/upload deve fazer upload multipart e retornar 201 Created")
    void deveFazerUploadDocumento() throws Exception {
        UUID docId = UUID.randomUUID();
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "plano_trabalho.pdf",
                "application/pdf",
                "dados do pdf".getBytes()
        );

        Documento docCriado = Documento.criarNovo(
                docId,
                tenantId,
                prefeituraId,
                convenioId,
                FaseCicloVida.FASE_01_CELEBRACAO,
                CategoriaDocumento.PROPOSTA_PLANO_TRABALHO,
                "/01_Celebracao_e_Formalizacao",
                new ArmazenamentoArquivo("bucket", "key", "plano_trabalho.pdf", "application/pdf", 12L),
                "hash123",
                OrigemCanal.UPLOAD_MANUAL,
                List.of("plano"),
                null,
                null
        );

        when(ficheiroUseCase.uploadDocumento(any())).thenReturn(docCriado);

        mockMvc.perform(multipart("/api/v1/convenios/" + convenioId + "/ficheiro/upload")
                        .file(arquivo)
                        .param("fase", "01_Celebracao_e_Formalizacao")
                        .param("categoria", "PROPOSTA_PLANO_TRABALHO")
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(docId.toString()))
                .andExpect(jsonPath("$.nomeArquivoOriginal").value("plano_trabalho.pdf"))
                .andExpect(jsonPath("$.faseCicloVida").value("FASE_01_CELEBRACAO"));
    }

    @Test
    @DisplayName("GET /api/v1/documentos/{id}/preview deve retornar URL pré-assinada")
    void deveObterUrlPreview() throws Exception {
        UUID docId = UUID.randomUUID();
        String presignedUrl = "https://minio.govflow.local/bucket/doc.pdf?X-Amz-Signature=xyz";

        when(ficheiroUseCase.obterUrlPreview(docId)).thenReturn(presignedUrl);

        mockMvc.perform(get("/api/v1/documentos/" + docId + "/preview")
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentoId").value(docId.toString()))
                .andExpect(jsonPath("$.url").value(presignedUrl))
                .andExpect(jsonPath("$.expiraEmMinutos").value(15));
    }

    @Test
    @DisplayName("PATCH /api/v1/documentos/{id}/mover deve mover documento e retornar 200 OK")
    void deveMoverDocumento() throws Exception {
        UUID docId = UUID.randomUUID();
        MoverDocumentoRequest request = new MoverDocumentoRequest(
                "05_Execucao_Financeira_e_Pagamentos",
                "/05_Execucao_Financeira_e_Pagamentos",
                "Reclassificação de nota fiscal"
        );

        Documento docMovido = Documento.criarNovo(
                docId,
                tenantId,
                prefeituraId,
                convenioId,
                FaseCicloVida.FASE_05_EXECUCAO_FINANCEIRA,
                CategoriaDocumento.DOCUMENTO_HABIL,
                "/05_Execucao_Financeira_e_Pagamentos",
                new ArmazenamentoArquivo("bucket", "key", "nf.pdf", "application/pdf", 100L),
                "hash",
                OrigemCanal.UPLOAD_MANUAL,
                null,
                null,
                null
        );

        when(ficheiroUseCase.moverDocumento(any())).thenReturn(docMovido);

        mockMvc.perform(patch("/api/v1/documentos/" + docId + "/mover")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(docId.toString()))
                .andExpect(jsonPath("$.faseCicloVida").value("FASE_05_EXECUCAO_FINANCEIRA"));
    }

    @Test
    @DisplayName("DELETE /api/v1/documentos/{id} deve realizar exclusão lógica e retornar 204 No Content")
    void deveExcluirDocumentoLogicamente() throws Exception {
        UUID docId = UUID.randomUUID();
        ExcluirDocumentoRequest request = new ExcluirDocumentoRequest("Documento duplicado");

        mockMvc.perform(delete("/api/v1/documentos/" + docId)
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(ficheiroUseCase).excluirDocumento(any(GerenciarFicheiroDigitalUseCase.ExcluirDocumentoCommand.class));
    }

    @Test
    @DisplayName("GET /api/v1/documentos/{id}/historico-auditoria deve retornar lista de auditoria")
    void deveListarAuditoriaDocumento() throws Exception {
        UUID docId = UUID.randomUUID();
        var auditoria = br.com.govflow.core.domain.model.documento.DocumentoAuditoria.registrar(
                tenantId,
                docId,
                UUID.randomUUID(),
                "UPLOAD",
                "Upload inicial",
                null,
                "novo"
        );

        when(ficheiroUseCase.listarAuditoria(docId)).thenReturn(List.of(auditoria));

        mockMvc.perform(get("/api/v1/documentos/" + docId + "/historico-auditoria")
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].acao").value("UPLOAD"))
                .andExpect(jsonPath("$[0].justificativa").value("Upload inicial"));
    }
}
