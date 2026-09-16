package br.com.govflow.whatsapp.inbound.parser;

import br.com.govflow.whatsapp.inbound.dto.InboundMessageDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class EvolutionWebhookParserTest {

    private EvolutionWebhookParser parser;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        parser = new EvolutionWebhookParser();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Deve extrair com sucesso documento em anexo (PDF de medição) da Evolution API")
    void deveParsearMensagemComDocumento() throws Exception {
        JsonNode rootNode = loadJsonPayload("payloads/evolution_document_message.json");

        assertTrue(parser.supports("EVOLUTION", rootNode));

        Optional<InboundMessageDto> dtoOpt = parser.parse(rootNode);
        assertTrue(dtoOpt.isPresent());

        InboundMessageDto dto = dtoOpt.get();
        assertEquals("EVO_MSG_DOC_12345", dto.externalMessageId());
        assertEquals("5583999999999", dto.senderPhone());
        assertEquals("Cícero Lucena (Prefeito)", dto.senderName());
        assertEquals("DOCUMENT", dto.messageType());
        assertTrue(dto.isDocument());
        assertEquals("application/pdf", dto.mediaMimeType());
        assertEquals("NF_Medicao_Asfalto_04.pdf", dto.fileName());
        assertEquals(2048576L, dto.fileSizeBytes());
        assertEquals("http://minio:9000/evolution/doc_nf_001.pdf", dto.mediaUrl());
        assertEquals("Segue a medição e nota fiscal da pavimentação", dto.contentText());
        assertFalse(dto.fromMe());
    }

    @Test
    @DisplayName("Deve extrair com sucesso mensagem de áudio de voz da Evolution API")
    void deveParsearMensagemComAudio() throws Exception {
        JsonNode rootNode = loadJsonPayload("payloads/evolution_audio_message.json");

        Optional<InboundMessageDto> dtoOpt = parser.parse(rootNode);
        assertTrue(dtoOpt.isPresent());

        InboundMessageDto dto = dtoOpt.get();
        assertEquals("EVO_MSG_AUDIO_67890", dto.externalMessageId());
        assertEquals("5583999999999", dto.senderPhone());
        assertEquals("AUDIO", dto.messageType());
        assertTrue(dto.isAudio());
        assertTrue(dto.mediaMimeType().contains("audio/ogg"));
        assertEquals("http://minio:9000/evolution/audio_medicao_01.ogg", dto.mediaUrl());
    }

    @Test
    @DisplayName("Deve extrair com sucesso mensagem de texto simples")
    void deveParsearMensagemDeTexto() throws Exception {
        JsonNode rootNode = loadJsonPayload("payloads/evolution_text_message.json");

        Optional<InboundMessageDto> dtoOpt = parser.parse(rootNode);
        assertTrue(dtoOpt.isPresent());

        InboundMessageDto dto = dtoOpt.get();
        assertEquals("EVO_MSG_TEXT_11223", dto.externalMessageId());
        assertEquals("TEXT", dto.messageType());
        assertFalse(dto.isMedia());
        assertEquals("Bom dia, já enviei os documentos da medição de João Pessoa.", dto.contentText());
    }

    @Test
    @DisplayName("Deve ignorar mensagens enviadas pelo próprio bot (fromMe=true)")
    void deveIgnorarMensagemEnviadaPeloProprioBot() throws Exception {
        String json = """
                {
                  "event": "messages.upsert",
                  "data": {
                    "key": {
                      "remoteJid": "5583999999999@s.whatsapp.net",
                      "fromMe": true,
                      "id": "MSG_FROM_ME_123"
                    },
                    "message": { "conversation": "Mensagem do bot" }
                  }
                }
                """;
        JsonNode rootNode = objectMapper.readTree(json);

        Optional<InboundMessageDto> dtoOpt = parser.parse(rootNode);
        assertTrue(dtoOpt.isEmpty(), "Mensagens com fromMe=true devem ser ignoradas");
    }

    @Test
    @DisplayName("Deve ignorar eventos que não sejam de mensagens (ex: presence.update)")
    void deveIgnorarEventosDePresencaOuConexao() throws Exception {
        String json = """
                {
                  "event": "presence.update",
                  "data": { "id": "123" }
                }
                """;
        JsonNode rootNode = objectMapper.readTree(json);

        Optional<InboundMessageDto> dtoOpt = parser.parse(rootNode);
        assertTrue(dtoOpt.isEmpty());
    }

    private JsonNode loadJsonPayload(String resourcePath) throws Exception {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            assertNotNull(is, "Arquivo de payload de teste não encontrado: " + resourcePath);
            return objectMapper.readTree(is);
        }
    }
}
