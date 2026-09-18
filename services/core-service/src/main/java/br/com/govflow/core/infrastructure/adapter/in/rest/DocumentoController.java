package br.com.govflow.core.infrastructure.adapter.in.rest;

import br.com.govflow.core.application.port.in.AprovarDocumentoUseCase;
import br.com.govflow.core.application.port.in.ConsultarDocumentoUseCase;
import br.com.govflow.core.application.port.in.RejeitarDocumentoUseCase;
import br.com.govflow.core.domain.exception.DocumentoNaoEncontradoException;
import br.com.govflow.core.domain.model.AuditoriaRevisao;
import br.com.govflow.core.domain.model.DadosRevisaoAnalista;
import br.com.govflow.core.domain.model.Documento;
import br.com.govflow.core.domain.model.StatusDocumento;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.request.AprovarDocumentoRequest;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.request.RejeitarDocumentoRequest;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.response.AuditoriaRevisaoResponse;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.response.DocumentoResponse;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documentos")
@Tag(name = "Documentos", description = "Endpoints para conferência lado a lado, aprovação auditada e rejeição de documentos fiscais (Human-in-the-Loop)")
public class DocumentoController {

    private final ConsultarDocumentoUseCase consultarUseCase;
    private final AprovarDocumentoUseCase aprovarUseCase;
    private final RejeitarDocumentoUseCase rejeitarUseCase;
    private final DocumentoRestMapper mapper;

    public DocumentoController(ConsultarDocumentoUseCase consultarUseCase,
                               AprovarDocumentoUseCase aprovarUseCase,
                               RejeitarDocumentoUseCase rejeitarUseCase,
                               DocumentoRestMapper mapper) {
        this.consultarUseCase = consultarUseCase;
        this.aprovarUseCase = aprovarUseCase;
        this.rejeitarUseCase = rejeitarUseCase;
        this.mapper = mapper;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar documento por ID", description = "Retorna os dados consolidados do documento, incluindo sugestões da IA e bounding boxes para o visualizador de PDF")
    public ResponseEntity<DocumentoResponse> buscarPorId(@PathVariable UUID id) {
        Documento doc = consultarUseCase.buscarPorId(id)
                .orElseThrow(() -> new DocumentoNaoEncontradoException(id));
        return ResponseEntity.ok(mapper.toResponse(doc));
    }

    @GetMapping
    @Operation(summary = "Listar documentos", description = "Retorna lista paginada de documentos com filtro opcional por status")
    public ResponseEntity<PageResponse<DocumentoResponse>> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) StatusDocumento status) {

        List<Documento> documentos = consultarUseCase.listar(page, size, status);
        long total = consultarUseCase.contar(status);

        List<DocumentoResponse> items = documentos.stream()
                .map(mapper::toResponse)
                .toList();

        return ResponseEntity.ok(PageResponse.of(items, page, size, total));
    }

    @PutMapping("/{id}/aprovar")
    @Operation(summary = "Aprovar documento revisado", description = "Aprova a conferência do analista humano, gera diff de auditoria e avança status para PRONTO_PARA_TRANSFEREGOV")
    public ResponseEntity<DocumentoResponse> aprovar(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) UUID userIdHeader,
            @Valid @RequestBody AprovarDocumentoRequest request) {

        UUID analistaId = request.analistaId() != null ? request.analistaId() : userIdHeader;
        if (analistaId == null) {
            throw new IllegalArgumentException("O identificador do analista é obrigatório para registrar a aprovação.");
        }
        DadosRevisaoAnalista revisao = mapper.toDomain(request.revisao());

        AprovarDocumentoUseCase.AprovarDocumentoCommand command =
                new AprovarDocumentoUseCase.AprovarDocumentoCommand(
                        id,
                        analistaId,
                        revisao,
                        request.observacao()
                );

        AprovarDocumentoUseCase.ResultadoAprovacao resultado = aprovarUseCase.aprovar(command);
        return ResponseEntity.ok(mapper.toResponse(resultado.documento()));
    }

    @PutMapping("/{id}/rejeitar")
    @Operation(summary = "Rejeitar documento", description = "Rejeita o documento informando justificativa obrigatória e transiciona para REJEITADO")
    public ResponseEntity<DocumentoResponse> rejeitar(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) UUID userIdHeader,
            @Valid @RequestBody RejeitarDocumentoRequest request) {

        UUID analistaId = request.analistaId() != null ? request.analistaId() : userIdHeader;
        if (analistaId == null) {
            throw new IllegalArgumentException("O identificador do analista é obrigatório para registrar a rejeição.");
        }

        RejeitarDocumentoUseCase.RejeitarDocumentoCommand command =
                new RejeitarDocumentoUseCase.RejeitarDocumentoCommand(
                        id,
                        analistaId,
                        request.motivo()
                );

        RejeitarDocumentoUseCase.ResultadoRejeicao resultado = rejeitarUseCase.rejeitar(command);
        return ResponseEntity.ok(mapper.toResponse(resultado.documento()));
    }

    @GetMapping("/{id}/auditoria")
    @Operation(summary = "Histórico de auditoria do documento", description = "Retorna a trilha de auditoria completa com comparativo (diff) de valores alterados")
    public ResponseEntity<List<AuditoriaRevisaoResponse>> listarAuditoria(@PathVariable UUID id) {
        List<AuditoriaRevisao> auditorias = consultarUseCase.listarAuditorias(id);
        List<AuditoriaRevisaoResponse> response = auditorias.stream()
                .map(mapper::toAuditoriaResponse)
                .toList();
        return ResponseEntity.ok(response);
    }
}
