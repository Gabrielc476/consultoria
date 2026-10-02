package br.com.govflow.core.infrastructure.adapter.in.rest;

import br.com.govflow.core.application.port.in.TriagemUseCase;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.request.CadastrarContatoRequest;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.response.ContatoResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/contatos")
public class ContatoController {

    private final TriagemUseCase triagemUseCase;

    public ContatoController(TriagemUseCase triagemUseCase) {
        this.triagemUseCase = triagemUseCase;
    }

    @GetMapping
    public ResponseEntity<List<ContatoResponse>> listarContatos() {
        List<ContatoResponse> contatos = triagemUseCase.listarContatos();
        return ResponseEntity.ok(contatos);
    }

    @PostMapping
    public ResponseEntity<ContatoResponse> cadastrarContato(@Valid @RequestBody CadastrarContatoRequest request) {
        ContatoResponse response = triagemUseCase.cadastrarContato(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
