package br.com.govflow.core.infrastructure.adapter.in.rest;

import br.com.govflow.core.application.port.in.GerenciarAgenteUseCase;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.request.AtualizarAgenteRequest;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.request.CriarAgenteRequest;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.response.AgenteResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/agentes")
@Tag(name = "Agentes", description = "Endpoints protegidos para gestão da equipe de agentes, celulares e vínculos com prefeituras")
public class AgenteController {

    private final GerenciarAgenteUseCase gerenciarAgenteUseCase;

    public AgenteController(GerenciarAgenteUseCase gerenciarAgenteUseCase) {
        this.gerenciarAgenteUseCase = gerenciarAgenteUseCase;
    }

    @PostMapping
    @Operation(summary = "Cadastrar novo Agente", description = "Cria um novo analista/agente para a consultoria com celular e prefeituras atribuídas")
    public ResponseEntity<AgenteResponseDto> criar(@Valid @RequestBody CriarAgenteRequest request) {
        var command = new GerenciarAgenteUseCase.CriarAgenteCommand(
                request.nome(),
                request.email(),
                request.senha(),
                request.telefoneCelular(),
                request.prefeiturasIds()
        );

        var response = gerenciarAgenteUseCase.criar(command);
        URI location = URI.create("/api/v1/agentes/" + response.id());
        return ResponseEntity.created(location).body(AgenteResponseDto.fromDomain(response));
    }

    @GetMapping
    @Operation(summary = "Listar Agentes da Consultoria", description = "Retorna todos os agentes da consultoria autenticada com as prefeituras associadas")
    public ResponseEntity<List<AgenteResponseDto>> listar() {
        List<AgenteResponseDto> dtos = gerenciarAgenteUseCase.listar().stream()
                .map(AgenteResponseDto::fromDomain)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar Agente por ID", description = "Retorna os detalhes de um agente específico")
    public ResponseEntity<AgenteResponseDto> buscarPorId(@PathVariable UUID id) {
        var response = gerenciarAgenteUseCase.buscarPorId(id);
        return ResponseEntity.ok(AgenteResponseDto.fromDomain(response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar Agente", description = "Atualiza dados cadastrais, telefone celular, status e prefeituras do agente")
    public ResponseEntity<AgenteResponseDto> atualizar(@PathVariable UUID id, @Valid @RequestBody AtualizarAgenteRequest request) {
        var command = new GerenciarAgenteUseCase.AtualizarAgenteCommand(
                id,
                request.nome(),
                request.telefoneCelular(),
                request.ativo(),
                request.prefeiturasIds()
        );

        var response = gerenciarAgenteUseCase.atualizar(command);
        return ResponseEntity.ok(AgenteResponseDto.fromDomain(response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Inativar Agente", description = "Desativa o acesso do agente ao sistema")
    public ResponseEntity<Void> inativar(@PathVariable UUID id) {
        gerenciarAgenteUseCase.inativar(id);
        return ResponseEntity.noContent().build();
    }
}
