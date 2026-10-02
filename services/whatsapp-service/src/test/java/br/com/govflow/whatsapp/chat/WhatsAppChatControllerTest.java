package br.com.govflow.whatsapp.chat;

import br.com.govflow.whatsapp.chat.controller.WhatsAppChatController;
import br.com.govflow.whatsapp.chat.dto.ChatResumoDto;
import br.com.govflow.whatsapp.chat.dto.EnviarMensagemRequest;
import br.com.govflow.whatsapp.chat.dto.MensagemChatDto;
import br.com.govflow.whatsapp.chat.service.EvolutionChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WhatsAppChatController.class)
class WhatsAppChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EvolutionChatService evolutionChatService;

    @Test
    @DisplayName("Deve listar chats recentes da Evolution API")
    void deveListarChatsRecentes() throws Exception {
        ChatResumoDto chat = new ChatResumoDto(
                "5583999991234@s.whatsapp.net",
                "5583999991234",
                "Eng. Roberto",
                null,
                "Segue o boletim de medição",
                Instant.now(),
                true
        );

        when(evolutionChatService.getRecentChats()).thenReturn(List.of(chat));

        mockMvc.perform(get("/api/v1/whatsapp/chats-recentes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].phone").value("5583999991234"))
                .andExpect(jsonPath("$[0].name").value("Eng. Roberto"))
                .andExpect(jsonPath("$[0].isCadastrado").value(true));
    }

    @Test
    @DisplayName("Deve obter histórico de mensagens de uma conversa")
    void deveObterHistoricoMensagens() throws Exception {
        MensagemChatDto msg = new MensagemChatDto(
                UUID.randomUUID(),
                "EVO_MSG_1",
                "5583999991234",
                "Eng. Roberto",
                "CONTATO",
                false,
                "TEXT",
                "Bom dia, medição enviada",
                null,
                null,
                null,
                null,
                null,
                null,
                Instant.now(),
                "LIDO"
        );

        when(evolutionChatService.getMessagesForPhone(eq("5583999991234"), any())).thenReturn(List.of(msg));

        mockMvc.perform(get("/api/v1/whatsapp/conversas/5583999991234/mensagens")
                        .header("X-Tenant-Id", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].texto").value("Bom dia, medição enviada"))
                .andExpect(jsonPath("$[0].remetente").value("CONTATO"));
    }

    @Test
    @DisplayName("Deve sincronizar retroativamente as últimas 10 mensagens")
    void deveSincronizarHistorico() throws Exception {
        MensagemChatDto msg = new MensagemChatDto(
                UUID.randomUUID(),
                "EVO_MSG_HIST_1",
                "5583999991234",
                "Eng. Roberto",
                "CONTATO",
                false,
                "TEXT",
                "Mensagem retroativa sincronizada",
                null,
                null,
                null,
                null,
                null,
                null,
                Instant.now(),
                "LIDO"
        );

        when(evolutionChatService.importLast10Messages(eq("5583999991234"), any(), any())).thenReturn(List.of(msg));

        mockMvc.perform(post("/api/v1/whatsapp/conversas/5583999991234/sincronizar-historico")
                        .param("contatoId", UUID.randomUUID().toString())
                        .header("X-Tenant-Id", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].texto").value("Mensagem retroativa sincronizada"));
    }

    @Test
    @DisplayName("Deve enviar mensagem de texto para o contato via WhatsApp")
    void deveEnviarMensagem() throws Exception {
        MensagemChatDto enviada = new MensagemChatDto(
                UUID.randomUUID(),
                "OUT_12345",
                "5583999991234",
                "GovFlow",
                "USUARIO",
                true,
                "TEXT",
                "Recebido com sucesso!",
                null,
                null,
                null,
                null,
                null,
                null,
                Instant.now(),
                "ENTREGUE"
        );

        when(evolutionChatService.sendMessage(eq("5583999991234"), eq("Recebido com sucesso!"), any(), any()))
                .thenReturn(enviada);

        EnviarMensagemRequest req = new EnviarMensagemRequest("Recebido com sucesso!");

        mockMvc.perform(post("/api/v1/whatsapp/conversas/5583999991234/enviar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .header("X-Tenant-Id", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].texto").doesNotExist()) // é objeto único
                .andExpect(jsonPath("$.texto").value("Recebido com sucesso!"))
                .andExpect(jsonPath("$.fromMe").value(true));
    }
}
