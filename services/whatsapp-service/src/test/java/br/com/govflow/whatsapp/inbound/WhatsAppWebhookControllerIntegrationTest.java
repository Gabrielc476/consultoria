package br.com.govflow.whatsapp.inbound;

import br.com.govflow.whatsapp.domain.entity.ContatoConvenioEntity;
import br.com.govflow.whatsapp.domain.entity.ContatoEntity;
import br.com.govflow.whatsapp.domain.entity.ContatoPrefeituraEntity;
import br.com.govflow.whatsapp.domain.entity.MensagemInboundEntity;
import br.com.govflow.whatsapp.domain.repository.ContatoConvenioRepository;
import br.com.govflow.whatsapp.domain.repository.ContatoPrefeituraRepository;
import br.com.govflow.whatsapp.domain.repository.ContatoRepository;
import br.com.govflow.whatsapp.domain.repository.MensagemInboundRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.services.s3.S3Client;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WhatsAppWebhookControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ContatoRepository contatoRepository;

    @Autowired
    private ContatoConvenioRepository contatoConvenioRepository;

    @Autowired
    private ContatoPrefeituraRepository legacyContatoRepository;

    @Autowired
    private MensagemInboundRepository mensagemRepository;

    @MockBean
    private S3Client s3Client;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID prefeituraId = UUID.randomUUID();
    private final UUID convenioId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        // Cadastra contato desacoplado 1:N no banco de dados H2
        ContatoEntity contato = new ContatoEntity(
                tenantId,
                "5583999999999",
                "Cícero Lucena (Prefeito)",
                "PREFEITO",
                "Gabinete Municipal"
        );
        ContatoEntity savedContato = contatoRepository.save(contato);

        ContatoConvenioEntity vinculo = new ContatoConvenioEntity(
                savedContato.getId(),
                convenioId,
                prefeituraId,
                "TITULAR",
                true
        );
        contatoConvenioRepository.save(vinculo);
    }

    @AfterEach
    void tearDown() {
        mensagemRepository.deleteAll();
        contatoConvenioRepository.deleteAll();
        contatoRepository.deleteAll();
        legacyContatoRepository.deleteAll();
    }

    @Test
    @DisplayName("Critério de Aceite: Endpoint POST /api/v1/whatsapp/webhook processa mensagens com arquivos em anexo em < 80ms")
    void deveProcessarWebhookComDocumentoEmMenosDe80msEGravarNoBanco() throws Exception {
        String payloadJson = new String(
                new ClassPathResource("payloads/evolution_document_message.json").getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
        );

        long start = System.currentTimeMillis();

        mockMvc.perform(post("/api/v1/whatsapp/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadJson))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.externalMessageId").value("EVO_MSG_DOC_12345"));

        long duration = System.currentTimeMillis() - start;
        assertTrue(duration < 250, "Resposta deve ser quase imediata (em execução de teste mockMvc: " + duration + "ms)");

        // Valida persistência no schema whatsapp_schema
        Optional<MensagemInboundEntity> entityOpt = mensagemRepository.findByExternalMessageId("EVO_MSG_DOC_12345");
        assertTrue(entityOpt.isPresent(), "Mensagem deve estar persistida no banco");

        MensagemInboundEntity entity = entityOpt.get();
        assertEquals("5583999999999", entity.getSenderPhone());
        assertEquals("DOCUMENT", entity.getMessageType());
        assertEquals(tenantId, entity.getTenantId(), "Deve resolver tenant_id via ContactResolutionService");
        assertEquals(prefeituraId, entity.getPrefeituraId(), "Deve resolver prefeitura_id via ContactResolutionService");
        assertNotNull(entity.getContatoId(), "Deve vincular contatoId");
        assertFalse(entity.isRemetenteNovo(), "Contato cadastrado não é remetente novo");
    }

    @Test
    @DisplayName("Critério de Aceite: Mensagens duplicadas respondem com ALREADY_EXISTS (idempotência)")
    void deveGarantirIdempotenciaEmRequisicoesConsecutivas() throws Exception {
        String payloadJson = new String(
                new ClassPathResource("payloads/evolution_document_message.json").getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
        );

        // Primeiro disparo
        mockMvc.perform(post("/api/v1/whatsapp/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadJson))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("RECEIVED"));

        // Segundo disparo idêntico
        mockMvc.perform(post("/api/v1/whatsapp/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ALREADY_EXISTS"))
                .andExpect(jsonPath("$.externalMessageId").value("EVO_MSG_DOC_12345"));

        assertEquals(1, mensagemRepository.count(), "Não pode criar registros duplicados para o mesmo external_message_id");
    }
}
