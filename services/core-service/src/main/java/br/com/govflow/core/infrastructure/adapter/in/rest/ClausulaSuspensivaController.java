package br.com.govflow.core.infrastructure.adapter.in.rest;

import br.com.govflow.core.application.port.in.*;
import br.com.govflow.core.domain.model.convenio.CondicionanteSuspensiva;
import br.com.govflow.core.domain.model.convenio.Convenio;
import br.com.govflow.core.domain.model.convenio.TipoCondicionanteSuspensiva;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.request.*;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.response.CondicionanteSuspensivaResponse;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.response.DossieClausulaSuspensivaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/convenios")
@Tag(name = "Cláusula Suspensiva (Fase 2)", description = "Gestão e superação dos três pilares da Caixa GIGOV (Engenharia, Ambiental e Titularidade)")
public class ClausulaSuspensivaController {

    private final ConsultarClausulaSuspensivaUseCase consultarUseCase;
    private final GerenciarCondicionanteUseCase gerenciarUseCase;
    private final ProrrogarPrazoClausulaSuspensivaUseCase prorrogarUseCase;
    private final SuperarClausulaSuspensivaUseCase superarUseCase;
    private final UploadDocumentoCondicionanteUseCase uploadUseCase;

    public ClausulaSuspensivaController(ConsultarClausulaSuspensivaUseCase consultarUseCase,
                                        GerenciarCondicionanteUseCase gerenciarUseCase,
                                        ProrrogarPrazoClausulaSuspensivaUseCase prorrogarUseCase,
                                        SuperarClausulaSuspensivaUseCase superarUseCase,
                                        UploadDocumentoCondicionanteUseCase uploadUseCase) {
        this.consultarUseCase = consultarUseCase;
        this.gerenciarUseCase = gerenciarUseCase;
        this.prorrogarUseCase = prorrogarUseCase;
        this.superarUseCase = superarUseCase;
        this.uploadUseCase = uploadUseCase;
    }

    @GetMapping("/{convenioId}/clausula-suspensiva")
    @Operation(summary = "Obter Dossiê da Cláusula Suspensiva por ID do Convênio",
            description = "Retorna status dos 3 pilares da Caixa, prazo fatal, dias restantes e semáforo de risco")
    public ResponseEntity<DossieClausulaSuspensivaResponse> obterDossiePorConvenioId(@PathVariable UUID convenioId) {
        var dossie = consultarUseCase.obterDossiePorConvenioId(convenioId);
        return ResponseEntity.ok(DossieClausulaSuspensivaResponse.fromDto(dossie));
    }

    @GetMapping("/siconv/{numeroSiconv}/clausula-suspensiva")
    @Operation(summary = "Obter Dossiê da Cláusula Suspensiva por Número SICONV",
            description = "Consulta dossiê pelo identificador Transferegov (ex: 914250/2023)")
    public ResponseEntity<DossieClausulaSuspensivaResponse> obterDossiePorNumeroSiconv(@PathVariable String numeroSiconv) {
        var dossie = consultarUseCase.obterDossiePorNumeroSiconv(numeroSiconv);
        return ResponseEntity.ok(DossieClausulaSuspensivaResponse.fromDto(dossie));
    }

    @PostMapping("/{convenioId}/clausula-suspensiva/condicionantes/{tipo}/submeter")
    @Operation(summary = "Submeter Pilar para Auditoria Técnica da Caixa GIGOV",
            description = "Transiciona status da condicionante para EM_ANALISE_CAIXA")
    public ResponseEntity<CondicionanteSuspensivaResponse> submeterParaAnalise(
            @PathVariable UUID convenioId,
            @PathVariable TipoCondicionanteSuspensiva tipo) {
        CondicionanteSuspensiva salva = gerenciarUseCase.submeterParaAnaliseCaixa(convenioId, tipo);
        return ResponseEntity.ok(CondicionanteSuspensivaResponse.fromDomain(salva));
    }

    @PostMapping("/{convenioId}/clausula-suspensiva/condicionantes/{tipo}/diligencia")
    @Operation(summary = "Registrar Diligência Emitida pela Caixa",
            description = "Registra laudo de pendências e data limite fatal para saneamento pelo município")
    public ResponseEntity<CondicionanteSuspensivaResponse> registrarDiligencia(
            @PathVariable UUID convenioId,
            @PathVariable TipoCondicionanteSuspensiva tipo,
            @Valid @RequestBody RegistrarDiligenciaRequest request) {
        var command = new GerenciarCondicionanteUseCase.RegistrarDiligenciaCommand(
                convenioId,
                tipo,
                request.observacoes(),
                request.s3KeyLaudoPendencias(),
                request.dataLimiteSaneamento()
        );
        CondicionanteSuspensiva salva = gerenciarUseCase.registrarDiligenciaCaixa(command);
        return ResponseEntity.ok(CondicionanteSuspensivaResponse.fromDomain(salva));
    }

    @PostMapping("/{convenioId}/clausula-suspensiva/condicionantes/{tipo}/aprovar")
    @Operation(summary = "Aprovar Pilar Técnico da Caixa",
            description = "Registra a emissão do LAE/SPA, Licença Ambiental ou Matrícula CRI aprovada")
    public ResponseEntity<CondicionanteSuspensivaResponse> aprovarCondicionante(
            @PathVariable UUID convenioId,
            @PathVariable TipoCondicionanteSuspensiva tipo,
            @Valid @RequestBody AprovarCondicionanteRequest request) {
        var command = new GerenciarCondicionanteUseCase.AprovarCondicionanteCommand(
                convenioId,
                tipo,
                request.numeroDocumentoComprobatorio(),
                request.dataAprovacao(),
                request.dataValidade(),
                request.valorOrcamentoAprovado(),
                request.percentualBdiAprovado(),
                request.numeroArtRrt(),
                request.orgaoEmissor(),
                request.s3KeyDocumento()
        );
        CondicionanteSuspensiva salva = gerenciarUseCase.aprovarCondicionante(command);
        return ResponseEntity.ok(CondicionanteSuspensivaResponse.fromDomain(salva));
    }

    @PutMapping("/{convenioId}/clausula-suspensiva/condicionantes/{tipo}")
    @Operation(summary = "Atualizar Parâmetros Técnicos da Condicionante")
    public ResponseEntity<CondicionanteSuspensivaResponse> atualizarParametros(
            @PathVariable UUID convenioId,
            @PathVariable TipoCondicionanteSuspensiva tipo,
            @Valid @RequestBody AtualizarCondicionanteRequest request) {
        var command = new GerenciarCondicionanteUseCase.AtualizarCondicionanteCommand(
                convenioId,
                tipo,
                request.numeroDocumentoComprobatorio(),
                request.dataValidade(),
                request.orgaoEmissor(),
                request.valorOrcamentoAprovado(),
                request.percentualBdiAprovado(),
                request.numeroArtRrt(),
                request.observacoes()
        );
        CondicionanteSuspensiva salva = gerenciarUseCase.atualizarParametrosTecnicos(command);
        return ResponseEntity.ok(CondicionanteSuspensivaResponse.fromDomain(salva));
    }

    @PostMapping(value = "/{convenioId}/clausula-suspensiva/condicionantes/{tipo}/documentos",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload de Documento Comprobatório no MinIO",
            description = "Armazena laudo, licença ou certidão no MinIO e vincula à condicionante")
    public ResponseEntity<CondicionanteSuspensivaResponse> uploadDocumento(
            @PathVariable UUID convenioId,
            @PathVariable TipoCondicionanteSuspensiva tipo,
            @RequestParam("arquivo") MultipartFile arquivo) throws IOException {
        CondicionanteSuspensiva salva = uploadUseCase.uploadDocumentoComprobatorio(
                convenioId,
                tipo,
                arquivo.getOriginalFilename(),
                arquivo.getContentType(),
                arquivo.getBytes()
        );
        return ResponseEntity.ok(CondicionanteSuspensivaResponse.fromDomain(salva));
    }

    @PostMapping(value = "/{convenioId}/clausula-suspensiva/condicionantes/{tipo}/laudo-pendencias",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload de Laudo de Pendências da Caixa no MinIO")
    public ResponseEntity<CondicionanteSuspensivaResponse> uploadLaudoPendencias(
            @PathVariable UUID convenioId,
            @PathVariable TipoCondicionanteSuspensiva tipo,
            @RequestParam("arquivo") MultipartFile arquivo) throws IOException {
        CondicionanteSuspensiva salva = uploadUseCase.uploadLaudoPendencias(
                convenioId,
                tipo,
                arquivo.getOriginalFilename(),
                arquivo.getContentType(),
                arquivo.getBytes()
        );
        return ResponseEntity.ok(CondicionanteSuspensivaResponse.fromDomain(salva));
    }

    @PostMapping("/{convenioId}/clausula-suspensiva/prorrogacao")
    @Operation(summary = "Solicitar Prorrogação de Prazo da Cláusula Suspensiva",
            description = "Registra protocolo de prorrogação excepcional com novo prazo proposto")
    public ResponseEntity<DossieClausulaSuspensivaResponse> solicitarProrrogacao(
            @PathVariable UUID convenioId,
            @Valid @RequestBody SolicitarProrrogacaoRequest request) {
        prorrogarUseCase.solicitarProrrogacao(convenioId, request.novoPrazoProrrogado());
        var dossie = consultarUseCase.obterDossiePorConvenioId(convenioId);
        return ResponseEntity.ok(DossieClausulaSuspensivaResponse.fromDto(dossie));
    }

    @PostMapping("/{convenioId}/clausula-suspensiva/superar")
    @Operation(summary = "Superar Cláusula Suspensiva e Destravar Fase 3",
            description = "Valida os 3 pilares aprovados, registra o Termo de Retirada e publica evento RabbitMQ")
    public ResponseEntity<DossieClausulaSuspensivaResponse> superarClausula(
            @PathVariable UUID convenioId,
            @Valid @RequestBody SuperarClausulaSuspensivaRequest request) {
        superarUseCase.superarClausulaSuspensiva(convenioId, request.s3KeyTermoRetirada());
        var dossie = consultarUseCase.obterDossiePorConvenioId(convenioId);
        return ResponseEntity.ok(DossieClausulaSuspensivaResponse.fromDto(dossie));
    }

    @PostMapping(value = "/{convenioId}/clausula-suspensiva/termo-retirada",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload do Termo de Retirada da Cláusula Suspensiva e Superação Imediata",
            description = "Faz upload do termo assinado no MinIO e supera a Cláusula Suspensiva em um único passo")
    public ResponseEntity<DossieClausulaSuspensivaResponse> uploadTermoRetirada(
            @PathVariable UUID convenioId,
            @RequestParam("arquivo") MultipartFile arquivo) throws IOException {
        uploadUseCase.uploadTermoRetirada(
                convenioId,
                arquivo.getOriginalFilename(),
                arquivo.getContentType(),
                arquivo.getBytes()
        );
        var dossie = consultarUseCase.obterDossiePorConvenioId(convenioId);
        return ResponseEntity.ok(DossieClausulaSuspensivaResponse.fromDto(dossie));
    }
}
