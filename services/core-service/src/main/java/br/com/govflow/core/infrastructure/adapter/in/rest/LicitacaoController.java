package br.com.govflow.core.infrastructure.adapter.in.rest;

import br.com.govflow.core.application.port.in.CadastrarLicitacaoUseCase;
import br.com.govflow.core.application.port.in.ConsultarLicitacoesConvenioUseCase;
import br.com.govflow.core.application.port.in.EmitirAioUseCase;
import br.com.govflow.core.application.port.in.HomologarLicitacaoUseCase;
import br.com.govflow.core.application.port.in.RegistrarResultadoVrplUseCase;
import br.com.govflow.core.application.port.in.SubmeterVrplUseCase;
import br.com.govflow.core.application.port.in.ValidarAioParaMedicaoUseCase;
import br.com.govflow.core.domain.model.licitacao.Licitacao;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.request.CadastrarLicitacaoRequest;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.request.EmitirAioRequest;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.request.HomologarLicitacaoRequest;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.request.RegistrarResultadoVrplRequest;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.request.SubmeterVrplRequest;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.response.LicitacaoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/convenios/{convenioId}/licitacoes")
@Tag(name = "Licitações (Fase 3)", description = "Certames 1:N, protocolo de VRPL, parecer da Caixa e emissão de AIO")
public class LicitacaoController {

    private final CadastrarLicitacaoUseCase cadastrarUseCase;
    private final ConsultarLicitacoesConvenioUseCase consultarUseCase;
    private final HomologarLicitacaoUseCase homologarUseCase;
    private final SubmeterVrplUseCase submeterVrplUseCase;
    private final RegistrarResultadoVrplUseCase registrarResultadoVrplUseCase;
    private final EmitirAioUseCase emitirAioUseCase;
    private final ValidarAioParaMedicaoUseCase validarAioParaMedicaoUseCase;

    public LicitacaoController(CadastrarLicitacaoUseCase cadastrarUseCase,
                               ConsultarLicitacoesConvenioUseCase consultarUseCase,
                               HomologarLicitacaoUseCase homologarUseCase,
                               SubmeterVrplUseCase submeterVrplUseCase,
                               RegistrarResultadoVrplUseCase registrarResultadoVrplUseCase,
                               EmitirAioUseCase emitirAioUseCase,
                               ValidarAioParaMedicaoUseCase validarAioParaMedicaoUseCase) {
        this.cadastrarUseCase = cadastrarUseCase;
        this.consultarUseCase = consultarUseCase;
        this.homologarUseCase = homologarUseCase;
        this.submeterVrplUseCase = submeterVrplUseCase;
        this.registrarResultadoVrplUseCase = registrarResultadoVrplUseCase;
        this.emitirAioUseCase = emitirAioUseCase;
        this.validarAioParaMedicaoUseCase = validarAioParaMedicaoUseCase;
    }

    @PostMapping
    @Operation(summary = "Cadastrar licitação vinculada ao convênio")
    public ResponseEntity<LicitacaoResponse> cadastrar(
            @PathVariable UUID convenioId,
            @Valid @RequestBody CadastrarLicitacaoRequest request) {
        Licitacao salva = cadastrarUseCase.cadastrar(new CadastrarLicitacaoUseCase.CadastrarLicitacaoCommand(
                convenioId,
                request.numeroProcessoAdministrativo(),
                request.numeroLicitacao(),
                request.modalidade(),
                request.criterioJulgamento(),
                request.regimeExecucao(),
                request.objeto(),
                request.valorEstimado(),
                request.dataPublicacaoEdital(),
                request.dataAberturaPropostas(),
                request.linkPncp(),
                request.linkTransferegov(),
                request.linkSistemaCompras()
        ));
        URI location = URI.create("/api/v1/convenios/" + convenioId + "/licitacoes/" + salva.getId());
        return ResponseEntity.created(location).body(LicitacaoResponse.fromDomain(salva));
    }

    @GetMapping
    @Operation(summary = "Listar licitações do convênio (relação 1:N)")
    public ResponseEntity<List<LicitacaoResponse>> listar(@PathVariable UUID convenioId) {
        List<LicitacaoResponse> itens = consultarUseCase.listarPorConvenio(convenioId).stream()
                .map(LicitacaoResponse::fromDomain)
                .toList();
        return ResponseEntity.ok(itens);
    }

    @GetMapping("/{licitacaoId}")
    @Operation(summary = "Obter detalhe da licitação")
    public ResponseEntity<LicitacaoResponse> buscar(
            @PathVariable UUID convenioId,
            @PathVariable UUID licitacaoId) {
        return ResponseEntity.ok(LicitacaoResponse.fromDomain(consultarUseCase.buscarPorId(convenioId, licitacaoId)));
    }

    @PostMapping("/{licitacaoId}/homologar")
    @Operation(summary = "Homologar certame e calcular economia de licitação")
    public ResponseEntity<LicitacaoResponse> homologar(
            @PathVariable UUID convenioId,
            @PathVariable UUID licitacaoId,
            @Valid @RequestBody HomologarLicitacaoRequest request) {
        Licitacao salva = homologarUseCase.homologar(new HomologarLicitacaoUseCase.HomologarLicitacaoCommand(
                convenioId,
                licitacaoId,
                request.valorHomologado(),
                request.dataHomologacao(),
                request.cnpjVencedor(),
                request.razaoSocialVencedor()
        ));
        return ResponseEntity.ok(LicitacaoResponse.fromDomain(salva));
    }

    @PostMapping("/{licitacaoId}/vrpl/submeter")
    @Operation(summary = "Submeter VRPL com protocolo Transferegov")
    public ResponseEntity<LicitacaoResponse> submeterVrpl(
            @PathVariable UUID convenioId,
            @PathVariable UUID licitacaoId,
            @Valid @RequestBody SubmeterVrplRequest request) {
        Licitacao salva = submeterVrplUseCase.submeter(new SubmeterVrplUseCase.SubmeterVrplCommand(
                convenioId,
                licitacaoId,
                request.numeroVrplTransferegov(),
                request.dataEnvioVrpl()
        ));
        return ResponseEntity.ok(LicitacaoResponse.fromDomain(salva));
    }

    @PostMapping("/{licitacaoId}/vrpl/resultado")
    @Operation(summary = "Registrar parecer técnico da Caixa (aceite, diligência ou rejeição)")
    public ResponseEntity<LicitacaoResponse> registrarResultadoVrpl(
            @PathVariable UUID convenioId,
            @PathVariable UUID licitacaoId,
            @Valid @RequestBody RegistrarResultadoVrplRequest request) {
        Licitacao salva = registrarResultadoVrplUseCase.registrar(new RegistrarResultadoVrplUseCase.RegistrarResultadoVrplCommand(
                convenioId,
                licitacaoId,
                request.resultado(),
                request.parecerTecnico(),
                request.observacoesDiligencia(),
                request.dataAceiteVrpl()
        ));
        return ResponseEntity.ok(LicitacaoResponse.fromDomain(salva));
    }

    @PostMapping("/{licitacaoId}/aio")
    @Operation(summary = "Registrar emissão da AIO")
    public ResponseEntity<LicitacaoResponse> emitirAio(
            @PathVariable UUID convenioId,
            @PathVariable UUID licitacaoId,
            @Valid @RequestBody EmitirAioRequest request) {
        Licitacao salva = emitirAioUseCase.emitir(new EmitirAioUseCase.EmitirAioCommand(
                convenioId,
                licitacaoId,
                request.numeroAio(),
                request.dataEmissaoAio()
        ));
        return ResponseEntity.ok(LicitacaoResponse.fromDomain(salva));
    }

    @PostMapping("/{licitacaoId}/validar-aio-medicao")
    @Operation(summary = "TravaAntigrosa: 204 se AIO emitida, 422 caso contrário")
    public ResponseEntity<Void> validarAioParaMedicao(
            @PathVariable UUID convenioId,
            @PathVariable UUID licitacaoId) {
        consultarUseCase.buscarPorId(convenioId, licitacaoId);
        validarAioParaMedicaoUseCase.garantirAioEmitida(licitacaoId);
        return ResponseEntity.noContent().build();
    }
}
