package br.com.govflow.core.infrastructure.adapter.in.rest;

import br.com.govflow.core.application.port.in.TriagemUseCase;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.request.CadastrarContatoETriarRequest;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.response.TriagemItemResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/triagem")
public class TriagemController {

    private final TriagemUseCase triagemUseCase;

    public TriagemController(TriagemUseCase triagemUseCase) {
        this.triagemUseCase = triagemUseCase;
    }

    @GetMapping("/pendentes")
    public ResponseEntity<List<TriagemItemResponse>> listarPendentes() {
        List<TriagemItemResponse> pendentes = triagemUseCase.listarPendentes();
        return ResponseEntity.ok(pendentes);
    }

    @PostMapping("/{inboxId}/cadastrar-contato-e-arquivar")
    public ResponseEntity<TriagemItemResponse> cadastrarContatoETriar(
            @PathVariable UUID inboxId,
            @Valid @RequestBody CadastrarContatoETriarRequest request
    ) {
        TriagemItemResponse response = triagemUseCase.cadastrarContatoETriar(inboxId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{inboxId}/confirmar-arquivamento")
    public ResponseEntity<TriagemItemResponse> confirmarArquivamento(
            @PathVariable UUID inboxId,
            @RequestParam(required = false) UUID convenioId,
            @RequestParam(required = false) String faseCicloVida
    ) {
        TriagemItemResponse response = triagemUseCase.confirmarArquivamento(inboxId, convenioId, faseCicloVida);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{inboxId}/ignorar")
    public ResponseEntity<TriagemItemResponse> ignorarItem(@PathVariable UUID inboxId) {
        TriagemItemResponse response = triagemUseCase.ignorarItem(inboxId);
        return ResponseEntity.ok(response);
    }
}
