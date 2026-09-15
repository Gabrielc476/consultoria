package br.com.govflow.core.infrastructure.adapter.in.rest;

import br.com.govflow.core.application.port.in.AtualizarPrefeituraUseCase;
import br.com.govflow.core.application.port.in.CadastrarPrefeituraUseCase;
import br.com.govflow.core.application.port.in.ConsultarPrefeituraUseCase;
import br.com.govflow.core.domain.exception.PrefeituraNaoEncontradaException;
import br.com.govflow.core.domain.model.Prefeitura;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.request.AtualizarPrefeituraRequest;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.request.CadastrarPrefeituraRequest;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.response.PageResponse;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.response.PrefeituraResponse;
import br.com.govflow.core.infrastructure.interceptor.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/prefeituras")
@Tag(name = "Prefeituras", description = "Endpoints REST protegidos para cadastro e consulta de Prefeituras Convenentes")
public class PrefeituraController {

    private final CadastrarPrefeituraUseCase cadastrarPrefeituraUseCase;
    private final ConsultarPrefeituraUseCase consultarPrefeituraUseCase;
    private final AtualizarPrefeituraUseCase atualizarPrefeituraUseCase;

    public PrefeituraController(CadastrarPrefeituraUseCase cadastrarPrefeituraUseCase,
                                ConsultarPrefeituraUseCase consultarPrefeituraUseCase,
                                AtualizarPrefeituraUseCase atualizarPrefeituraUseCase) {
        this.cadastrarPrefeituraUseCase = cadastrarPrefeituraUseCase;
        this.consultarPrefeituraUseCase = consultarPrefeituraUseCase;
        this.atualizarPrefeituraUseCase = atualizarPrefeituraUseCase;
    }

    @PostMapping
    @Operation(summary = "Cadastrar nova Prefeitura", description = "Cadastra uma prefeitura vinculada à consultoria autenticada (X-Tenant-Id)")
    public ResponseEntity<PrefeituraResponse> cadastrar(@Valid @RequestBody CadastrarPrefeituraRequest request) {
        UUID tenantId = TenantContext.getCurrentTenant();

        CadastrarPrefeituraUseCase.CadastrarPrefeituraCommand command =
                new CadastrarPrefeituraUseCase.CadastrarPrefeituraCommand(
                        tenantId,
                        request.cnpj(),
                        request.razaoSocial(),
                        request.nomeMunicipio(),
                        request.uf(),
                        request.codigoIbge(),
                        request.porteMunicipio(),
                        request.nomePrefeito(),
                        request.cpfPrefeito(),
                        request.inicioMandato(),
                        request.fimMandato()
                );

        Prefeitura prefeitura = cadastrarPrefeituraUseCase.cadastrar(command);
        URI location = URI.create("/api/v1/prefeituras/" + prefeitura.getId());

        return ResponseEntity.created(location).body(PrefeituraResponse.fromDomain(prefeitura));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar Prefeitura por ID", description = "Retorna os dados cadastrais da prefeitura")
    public ResponseEntity<PrefeituraResponse> buscarPorId(@PathVariable UUID id) {
        Prefeitura prefeitura = consultarPrefeituraUseCase.buscarPorId(id)
                .orElseThrow(() -> new PrefeituraNaoEncontradaException(id));

        return ResponseEntity.ok(PrefeituraResponse.fromDomain(prefeitura));
    }

    @GetMapping
    @Operation(summary = "Listar Prefeituras", description = "Retorna a listagem paginada de prefeituras da consultoria com filtro opcional por status ativo")
    public ResponseEntity<PageResponse<PrefeituraResponse>> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean ativo) {

        List<Prefeitura> prefeituras = consultarPrefeituraUseCase.listar(page, size, ativo);
        long total = consultarPrefeituraUseCase.contar(ativo);

        List<PrefeituraResponse> responses = prefeituras.stream()
                .map(PrefeituraResponse::fromDomain)
                .toList();

        return ResponseEntity.ok(PageResponse.of(responses, page, size, total));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar Prefeitura", description = "Atualiza os dados cadastrais da prefeitura existente")
    public ResponseEntity<PrefeituraResponse> atualizar(@PathVariable UUID id,
                                                        @Valid @RequestBody AtualizarPrefeituraRequest request) {
        AtualizarPrefeituraUseCase.AtualizarPrefeituraCommand command =
                new AtualizarPrefeituraUseCase.AtualizarPrefeituraCommand(
                        request.razaoSocial(),
                        request.nomeMunicipio(),
                        request.porteMunicipio(),
                        request.nomePrefeito(),
                        request.cpfPrefeito(),
                        request.inicioMandato(),
                        request.fimMandato(),
                        request.statusCauc()
                );

        Prefeitura atualizada = atualizarPrefeituraUseCase.atualizar(id, command);
        return ResponseEntity.ok(PrefeituraResponse.fromDomain(atualizada));
    }

    @PatchMapping("/{id}/inativar")
    @Operation(summary = "Inativar Prefeitura", description = "Desativa o monitoramento da prefeitura pela consultoria")
    public ResponseEntity<Void> inativar(@PathVariable UUID id) {
        atualizarPrefeituraUseCase.inativar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/ativar")
    @Operation(summary = "Ativar Prefeitura", description = "Reativa o monitoramento da prefeitura pela consultoria")
    public ResponseEntity<Void> ativar(@PathVariable UUID id) {
        atualizarPrefeituraUseCase.ativar(id);
        return ResponseEntity.noContent().build();
    }
}
