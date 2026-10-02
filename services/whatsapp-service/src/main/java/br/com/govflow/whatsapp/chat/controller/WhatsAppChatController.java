package br.com.govflow.whatsapp.chat.controller;

import br.com.govflow.whatsapp.chat.dto.ChatResumoDto;
import br.com.govflow.whatsapp.chat.dto.EnviarMensagemRequest;
import br.com.govflow.whatsapp.chat.dto.MensagemChatDto;
import br.com.govflow.whatsapp.chat.service.EvolutionChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/whatsapp")
@Tag(name = "WhatsApp Central & Chat", description = "Endpoints para a Central de Mensagens do WhatsApp, chats recentes e envio")
public class WhatsAppChatController {

    private final EvolutionChatService evolutionChatService;

    public WhatsAppChatController(EvolutionChatService evolutionChatService) {
        this.evolutionChatService = evolutionChatService;
    }

    @GetMapping("/chats-recentes")
    @Operation(summary = "Lista os chats recentes da instância na Evolution API (apenas conversas individuais)")
    public ResponseEntity<List<ChatResumoDto>> listarChatsRecentes() {
        return ResponseEntity.ok(evolutionChatService.getRecentChats());
    }

    @GetMapping("/conversas/{phoneNumber}/mensagens")
    @Operation(summary = "Retorna o histórico de mensagens de um contato cadastrado")
    public ResponseEntity<List<MensagemChatDto>> obterMensagensConversa(
            @PathVariable String phoneNumber,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantIdHeader
    ) {
        UUID tenantId = tenantIdHeader != null ? UUID.fromString(tenantIdHeader) : null;
        return ResponseEntity.ok(evolutionChatService.getMessagesForPhone(phoneNumber, tenantId));
    }

    @PostMapping("/conversas/{phoneNumber}/sincronizar-historico")
    @Operation(summary = "Sincroniza retroativamente as últimas 10 mensagens do contato na Evolution API")
    public ResponseEntity<List<MensagemChatDto>> sincronizarHistorico(
            @PathVariable String phoneNumber,
            @RequestParam(required = false) UUID contatoId,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantIdHeader
    ) {
        UUID tenantId = tenantIdHeader != null ? UUID.fromString(tenantIdHeader) : null;
        return ResponseEntity.ok(evolutionChatService.importLast10Messages(phoneNumber, tenantId, contatoId));
    }

    @PostMapping("/conversas/{phoneNumber}/enviar")
    @Operation(summary = "Envia uma mensagem de texto para o contato via WhatsApp e registra no histórico")
    public ResponseEntity<MensagemChatDto> enviarMensagem(
            @PathVariable String phoneNumber,
            @Valid @RequestBody EnviarMensagemRequest request,
            @RequestParam(required = false) UUID contatoId,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantIdHeader
    ) {
        UUID tenantId = tenantIdHeader != null ? UUID.fromString(tenantIdHeader) : null;
        return ResponseEntity.ok(evolutionChatService.sendMessage(phoneNumber, request.texto(), tenantId, contatoId));
    }
}
