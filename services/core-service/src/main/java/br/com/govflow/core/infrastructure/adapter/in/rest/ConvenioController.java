package br.com.govflow.core.infrastructure.adapter.in.rest;

import br.com.govflow.core.application.port.in.CadastrarConvenioUseCase;
import br.com.govflow.core.application.port.in.ConsultarConveniosUseCase;
import br.com.govflow.core.domain.exception.ConvenioNaoEncontradoException;
import br.com.govflow.core.domain.model.convenio.Convenio;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.request.CadastrarConvenioRequest;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.response.ConvenioResponse;
import br.com.govflow.core.infrastructure.interceptor.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/convenios")
@Tag(name = "Convênios", description = "Endpoints REST protegidos para gestão, cadastro e consulta de Convênios e Contratos de Repasse")
public class ConvenioController {

    private final CadastrarConvenioUseCase cadastrarConvenioUseCase;
    private final ConsultarConveniosUseCase consultarConveniosUseCase;

    public ConvenioController(CadastrarConvenioUseCase cadastrarConvenioUseCase,
                              ConsultarConveniosUseCase consultarConveniosUseCase) {
        this.cadastrarConvenioUseCase = cadastrarConvenioUseCase;
        this.consultarConveniosUseCase = consultarConveniosUseCase;
    }

    @PostMapping
    @Operation(summary = "Cadastrar novo Convênio manual", description = "Cadastra um novo convênio vinculado à prefeitura e consultoria autenticada (X-Tenant-Id)")
    public ResponseEntity<ConvenioResponse> cadastrar(@Valid @RequestBody CadastrarConvenioRequest request) {
        UUID tenantId = TenantContext.getCurrentTenant();

        CadastrarConvenioUseCase.CadastrarConvenioCommand command =
                new CadastrarConvenioUseCase.CadastrarConvenioCommand(
                        tenantId,
                        request.prefeituraId(),
                        request.numeroSiconv(),
                        request.numeroProcesso(),
                        request.orgaoConcedente(),
                        request.objeto(),
                        request.valorGlobal(),
                        request.valorRepasse(),
                        request.valorContrapartida(),
                        request.possuiClausulaSuspensiva(),
                        request.prazoClausulaSuspensiva(),
                        request.dataInicioVigencia(),
                        request.dataFimVigencia()
                );

        Convenio convenio = cadastrarConvenioUseCase.cadastrar(command);
        URI location = URI.create("/api/v1/convenios/" + convenio.getId());

        return ResponseEntity.created(location).body(ConvenioResponse.fromDomain(convenio));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar Convênio por ID", description = "Retorna os detalhes cadastrais e operacionais do convênio")
    public ResponseEntity<ConvenioResponse> buscarPorId(@PathVariable UUID id) {
        Convenio convenio = consultarConveniosUseCase.buscarPorId(id)
                .orElseThrow(() -> new ConvenioNaoEncontradoException(id));

        return ResponseEntity.ok(ConvenioResponse.fromDomain(convenio));
    }

    @GetMapping
    @Operation(summary = "Listar Convênios", description = "Retorna a listagem de convênios com filtro opcional por prefeitura")
    public ResponseEntity<List<ConvenioResponse>> listar(
            @RequestParam(required = false) UUID prefeituraId) {
        UUID tenantId = TenantContext.getCurrentTenant();

        List<Convenio> convenios;
        if (prefeituraId != null) {
            convenios = consultarConveniosUseCase.listarPorPrefeitura(prefeituraId);
        } else {
            convenios = consultarConveniosUseCase.listarPorTenant(tenantId);
        }

        List<ConvenioResponse> responses = convenios.stream()
                .map(ConvenioResponse::fromDomain)
                .toList();

        return ResponseEntity.ok(responses);
    }
}
