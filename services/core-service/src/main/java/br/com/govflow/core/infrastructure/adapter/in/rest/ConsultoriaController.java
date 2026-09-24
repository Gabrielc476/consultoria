package br.com.govflow.core.infrastructure.adapter.in.rest;

import br.com.govflow.core.application.port.in.CadastrarConsultoriaUseCase;
import br.com.govflow.core.application.port.in.ConsultarConsultoriaUseCase;
import br.com.govflow.core.domain.exception.ConsultoriaNaoEncontradaException;
import br.com.govflow.core.domain.model.Consultoria;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.request.CadastrarConsultoriaRequest;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.response.ConsultoriaResponse;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/consultorias")
@Tag(name = "Consultorias", description = "Endpoints REST para cadastro e consulta de Consultorias (Tenants)")
public class ConsultoriaController {

    private final CadastrarConsultoriaUseCase cadastrarConsultoriaUseCase;
    private final ConsultarConsultoriaUseCase consultarConsultoriaUseCase;

    public ConsultoriaController(CadastrarConsultoriaUseCase cadastrarConsultoriaUseCase,
                                 ConsultarConsultoriaUseCase consultarConsultoriaUseCase) {
        this.cadastrarConsultoriaUseCase = cadastrarConsultoriaUseCase;
        this.consultarConsultoriaUseCase = consultarConsultoriaUseCase;
    }

    @PostMapping
    @Operation(summary = "Cadastrar nova Consultoria (Tenant)", description = "Cadastro inicial de empresa de consultoria privada detentora da assinatura (Tenant)")
    public ResponseEntity<ConsultoriaResponse> cadastrar(@Valid @RequestBody CadastrarConsultoriaRequest request) {
        CadastrarConsultoriaUseCase.CadastrarConsultoriaCommand command =
                new CadastrarConsultoriaUseCase.CadastrarConsultoriaCommand(
                        request.cnpj(),
                        request.razaoSocial(),
                        request.nomeFantasia(),
                        request.emailContato(),
                        request.telefoneContato(),
                        request.plano()
                );

        Consultoria consultoria = cadastrarConsultoriaUseCase.cadastrar(command);
        URI location = URI.create("/api/v1/consultorias/" + consultoria.getId());

        return ResponseEntity.created(location).body(ConsultoriaResponse.fromDomain(consultoria));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar Consultoria por ID", description = "Retorna os dados cadastrais da consultoria")
    public ResponseEntity<ConsultoriaResponse> buscarPorId(@PathVariable UUID id) {
        Consultoria consultoria = consultarConsultoriaUseCase.buscarPorId(id)
                .orElseThrow(() -> new ConsultoriaNaoEncontradaException(id));

        return ResponseEntity.ok(ConsultoriaResponse.fromDomain(consultoria));
    }
}
