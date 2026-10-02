package br.com.govflow.core.infrastructure.adapter.in.rest;

import br.com.govflow.core.application.port.in.ExportarFicheiroZipUseCase;
import br.com.govflow.core.application.port.in.GerenciarFicheiroDigitalUseCase;
import br.com.govflow.core.domain.model.Documento;
import br.com.govflow.core.domain.model.documento.CategoriaDocumento;
import br.com.govflow.core.domain.model.documento.FaseCicloVida;
import br.com.govflow.core.domain.model.documento.FicheiroDigital;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.request.ExcluirDocumentoRequest;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.request.MoverDocumentoRequest;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.response.DocumentoFicheiroResponse;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.response.FicheiroDigitalResponse;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.response.PreviewDocumentoResponse;
import br.com.govflow.core.infrastructure.interceptor.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Ficheiro Digital", description = "Endpoints de navegação na árvore de fases, upload, visualização inline e backup ZIP")
public class FicheiroDigitalController {

    private final GerenciarFicheiroDigitalUseCase ficheiroUseCase;
    private final ExportarFicheiroZipUseCase exportarZipUseCase;
    private final FicheiroRestMapper mapper;

    public FicheiroDigitalController(GerenciarFicheiroDigitalUseCase ficheiroUseCase,
                                   ExportarFicheiroZipUseCase exportarZipUseCase,
                                   FicheiroRestMapper mapper) {
        this.ficheiroUseCase = ficheiroUseCase;
        this.exportarZipUseCase = exportarZipUseCase;
        this.mapper = mapper;
    }

    @GetMapping("/api/v1/convenios/{convenioId}/ficheiro")
    @Operation(summary = "Obter árvore hierárquica do Ficheiro Digital", description = "Retorna a estrutura das 10 Fases do convênio com contadores de arquivos e totalizadores de bytes")
    public ResponseEntity<FicheiroDigitalResponse> obterFicheiro(@PathVariable UUID convenioId) {
        FicheiroDigital ficheiro = ficheiroUseCase.obterFicheiro(convenioId);
        return ResponseEntity.ok(mapper.toResponse(ficheiro));
    }

    @GetMapping("/api/v1/convenios/{convenioId}/ficheiro/fases/{fase}")
    @Operation(summary = "Listar documentos de uma fase específica", description = "Lista detalhadamente os documentos presentes na fase selecionada")
    public ResponseEntity<List<DocumentoFicheiroResponse>> listarDocumentosFase(
            @PathVariable UUID convenioId,
            @PathVariable String fase) {
        FaseCicloVida faseEnum = FaseCicloVida.fromCodigoOuNome(fase);
        List<Documento> docs = ficheiroUseCase.listarDocumentosFase(convenioId, faseEnum);
        List<DocumentoFicheiroResponse> response = docs.stream().map(mapper::toResponse).toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/api/v1/convenios/{convenioId}/ficheiro/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload manual de documento no Ficheiro Digital", description = "Armazena o arquivo no MinIO/S3 sob a hierarquia da fase e registra metadados com hash SHA-256")
    public ResponseEntity<DocumentoFicheiroResponse> uploadDocumento(
            @PathVariable UUID convenioId,
            @RequestParam("arquivo") MultipartFile arquivo,
            @RequestParam(value = "fase", required = false) String faseStr,
            @RequestParam(value = "categoria", required = false) String categoriaStr,
            @RequestParam(value = "pastaVirtual", required = false) String pastaVirtual,
            @RequestParam(value = "tags", required = false) List<String> tags,
            @RequestHeader(value = "X-User-Id", required = false) UUID userIdHeader) throws IOException {

        UUID usuarioId = userIdHeader != null ? userIdHeader : UserContext.getUserId();
        FaseCicloVida fase = FaseCicloVida.fromCodigoOuNome(faseStr);
        CategoriaDocumento categoria = CategoriaDocumento.fromString(categoriaStr);

        GerenciarFicheiroDigitalUseCase.UploadDocumentoCommand command =
                new GerenciarFicheiroDigitalUseCase.UploadDocumentoCommand(
                        convenioId,
                        fase,
                        categoria,
                        pastaVirtual,
                        arquivo.getOriginalFilename(),
                        arquivo.getContentType(),
                        arquivo.getBytes(),
                        tags,
                        usuarioId
                );

        Documento documento = ficheiroUseCase.uploadDocumento(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(documento));
    }

    @GetMapping("/api/v1/convenios/{convenioId}/ficheiro/download-zip")
    @Operation(summary = "Download do convênio completo em arquivo ZIP", description = "Compacta via streaming não bloqueante todos os documentos organizados fielmente nas 10 Fases")
    public ResponseEntity<StreamingResponseBody> downloadZipConvenio(
            @PathVariable UUID convenioId,
            HttpServletResponse response) {

        FicheiroDigital ficheiro = ficheiroUseCase.obterFicheiro(convenioId);
        String siconvClean = (ficheiro.getNumeroSiconv() != null)
                ? ficheiro.getNumeroSiconv().replaceAll("[^a-zA-Z0-9_-]", "_")
                : convenioId.toString();

        String filename = String.format("Dossie_Convenio_%s.zip", siconvClean);

        response.setContentType("application/zip");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");

        StreamingResponseBody stream = outputStream -> exportarZipUseCase.exportarConvenioIntegral(convenioId, outputStream);
        return ResponseEntity.ok(stream);
    }

    @GetMapping("/api/v1/convenios/{convenioId}/ficheiro/fases/{fase}/download-zip")
    @Operation(summary = "Download de pasta da fase em arquivo ZIP", description = "Compacta via streaming os documentos da fase selecionada")
    public ResponseEntity<StreamingResponseBody> downloadZipFase(
            @PathVariable UUID convenioId,
            @PathVariable String fase,
            HttpServletResponse response) {

        FaseCicloVida faseEnum = FaseCicloVida.fromCodigoOuNome(fase);
        String filename = String.format("Dossie_%s.zip", faseEnum.getNomePasta());

        response.setContentType("application/zip");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");

        StreamingResponseBody stream = outputStream -> exportarZipUseCase.exportarFaseEspecifica(convenioId, faseEnum, outputStream);
        return ResponseEntity.ok(stream);
    }

    @GetMapping("/api/v1/documentos/{id}/preview")
    @Operation(summary = "Obter URL pré-assinada para preview inline", description = "Retorna URL temporária (15 minutos) do MinIO para visualização direta no navegador sem download local")
    public ResponseEntity<PreviewDocumentoResponse> obterPreview(@PathVariable UUID id) {
        String url = ficheiroUseCase.obterUrlPreview(id);
        return ResponseEntity.ok(new PreviewDocumentoResponse(id, url, 15));
    }

    @GetMapping("/api/v1/documentos/{id}/conteudo")
    @Operation(summary = "Streaming direto do conteúdo do documento", description = "Retorna os bytes do documento com Content-Type original e cabeçalho inline")
    public ResponseEntity<StreamingResponseBody> obterConteudo(
            @PathVariable UUID id,
            HttpServletResponse response) {

        Documento doc = ficheiroUseCase.obterDocumentoPorId(id);
        String filename = (doc.getNomeArquivoOriginal() != null)
                ? doc.getNomeArquivoOriginal().replaceAll("[^a-zA-Z0-9._-]", "_")
                : "documento_" + id;

        response.setContentType(doc.getContentType() != null ? doc.getContentType() : "application/octet-stream");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"");

        StreamingResponseBody stream = outputStream -> ficheiroUseCase.escreverConteudoDocumento(id, outputStream);
        return ResponseEntity.ok(stream);
    }

    @PatchMapping("/api/v1/documentos/{id}/mover")
    @Operation(summary = "Mover documento de fase ou pasta virtual", description = "Atualiza a localização lógica do documento no Ficheiro Digital com registro de auditoria")
    public ResponseEntity<DocumentoFicheiroResponse> moverDocumento(
            @PathVariable UUID id,
            @Valid @RequestBody MoverDocumentoRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userIdHeader) {

        UUID usuarioId = userIdHeader != null ? userIdHeader : UserContext.getUserId();
        FaseCicloVida novaFase = FaseCicloVida.fromCodigoOuNome(request.novaFase());

        GerenciarFicheiroDigitalUseCase.MoverDocumentoCommand command =
                new GerenciarFicheiroDigitalUseCase.MoverDocumentoCommand(
                        id,
                        novaFase,
                        request.novaPastaVirtual(),
                        request.justificativa(),
                        usuarioId
                );

        Documento doc = ficheiroUseCase.moverDocumento(command);
        return ResponseEntity.ok(mapper.toResponse(doc));
    }

    @DeleteMapping("/api/v1/documentos/{id}")
    @Operation(summary = "Exclusão lógica de documento do Ficheiro Digital", description = "Marca o documento como excluído e registra justificativa de auditoria")
    public ResponseEntity<Void> excluirDocumento(
            @PathVariable UUID id,
            @RequestBody(required = false) ExcluirDocumentoRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userIdHeader) {

        UUID usuarioId = userIdHeader != null ? userIdHeader : UserContext.getUserId();
        String justificativa = request != null ? request.justificativa() : "Exclusão manual solicitada";

        GerenciarFicheiroDigitalUseCase.ExcluirDocumentoCommand command =
                new GerenciarFicheiroDigitalUseCase.ExcluirDocumentoCommand(
                        id,
                        justificativa,
                        usuarioId
                );

        ficheiroUseCase.excluirDocumento(command);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/documentos/{id}/historico-auditoria")
    @Operation(summary = "Histórico de auditoria do Ficheiro Digital", description = "Retorna o histórico imutável de movimentações, classificações e auditorias do documento")
    public ResponseEntity<List<br.com.govflow.core.infrastructure.adapter.in.rest.dto.response.DocumentoAuditoriaResponse>> listarAuditoria(
            @PathVariable UUID id) {
        var auditorias = ficheiroUseCase.listarAuditoria(id);
        var response = auditorias.stream().map(mapper::toResponse).toList();
        return ResponseEntity.ok(response);
    }
}
