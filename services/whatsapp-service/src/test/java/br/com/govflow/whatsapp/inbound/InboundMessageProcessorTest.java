package br.com.govflow.whatsapp.inbound;

import br.com.govflow.whatsapp.config.WhatsAppProperties;
import br.com.govflow.whatsapp.domain.entity.MensagemInboundEntity;
import br.com.govflow.whatsapp.domain.repository.MensagemInboundRepository;
import br.com.govflow.whatsapp.inbound.dto.InboundMessageDto;
import br.com.govflow.whatsapp.inbound.dto.WebhookResponseDto;
import br.com.govflow.whatsapp.inbound.event.InboundMessageReceivedInternalEvent;
import br.com.govflow.whatsapp.inbound.parser.WebhookParserStrategy;
import br.com.govflow.whatsapp.inbound.service.InboundMessageProcessor;
import br.com.govflow.whatsapp.routing.service.ContactResolutionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InboundMessageProcessorTest {

    @Mock
    private WebhookParserStrategy parser;

    @Mock
    private ContactResolutionService contactResolutionService;

    @Mock
    private MensagemInboundRepository mensagemRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private WhatsAppProperties properties;
    private InboundMessageProcessor processor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final UUID tenantId = UUID.randomUUID();
    private final UUID prefeituraId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        properties = new WhatsAppProperties();
        properties.setActiveProvider("EVOLUTION");

        processor = new InboundMessageProcessor(
                List.of(parser),
                contactResolutionService,
                mensagemRepository,
                eventPublisher,
                properties
        );
    }

    @Test
    @DisplayName("Primeiro recebimento de webhook: deve persistir e publicar evento interno para execução pós-commit")
    void deveProcessarPrimeiroRecebimentoComSucesso() throws Exception {
        JsonNode rootNode = objectMapper.readTree("{\"event\":\"messages.upsert\"}");
        InboundMessageDto dto = new InboundMessageDto(
                "govflow-consultoria",
                "MSG_UNIQUE_001",
                "5583999999999",
                "Prefeito",
                "DOCUMENT",
                "Nota Fiscal",
                "http://media/doc.pdf",
                "application/pdf",
                "nf.pdf",
                1024L,
                "{}",
                false
        );

        when(parser.supports(eq("EVOLUTION"), any(JsonNode.class))).thenReturn(true);
        when(parser.parse(any(JsonNode.class))).thenReturn(Optional.of(dto));
        when(mensagemRepository.existsByExternalMessageId("MSG_UNIQUE_001")).thenReturn(false);
        when(contactResolutionService.resolve("5583999999999")).thenReturn(
                new ContactResolutionService.ResolvedContact(tenantId, prefeituraId, "Prefeito", "PREFEITO", true)
        );

        UUID generatedId = UUID.randomUUID();
        when(mensagemRepository.save(any(MensagemInboundEntity.class))).thenAnswer(invocation -> {
            MensagemInboundEntity entity = invocation.getArgument(0);
            entity.setId(generatedId);
            return entity;
        });

        WebhookResponseDto response = processor.processWebhook(rootNode);

        assertEquals("RECEIVED", response.status());
        assertEquals("MSG_UNIQUE_001", response.externalMessageId());

        // Verifica que salvou no banco
        ArgumentCaptor<MensagemInboundEntity> captor = ArgumentCaptor.forClass(MensagemInboundEntity.class);
        verify(mensagemRepository, times(1)).save(captor.capture());
        MensagemInboundEntity saved = captor.getValue();
        assertEquals("MSG_UNIQUE_001", saved.getExternalMessageId());
        assertEquals(tenantId, saved.getTenantId());
        assertEquals(prefeituraId, saved.getPrefeituraId());
        assertFalse(saved.isProcessed());

        // Verifica publicação do evento interno pós-commit
        ArgumentCaptor<InboundMessageReceivedInternalEvent> eventCaptor =
                ArgumentCaptor.forClass(InboundMessageReceivedInternalEvent.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
        InboundMessageReceivedInternalEvent capturedEvent = eventCaptor.getValue();
        assertEquals(generatedId, capturedEvent.mensagemInboundId());
        assertEquals(tenantId, capturedEvent.tenantId());
        assertEquals(prefeituraId, capturedEvent.prefeituraId());
    }

    @Test
    @DisplayName("Idempotência: recebimento repetido da mesma mensagem deve retornar ALREADY_EXISTS sem reprocessar")
    void deveGarantirIdempotenciaEmMensagensDuplicadas() throws Exception {
        JsonNode rootNode = objectMapper.readTree("{\"event\":\"messages.upsert\"}");
        InboundMessageDto dto = new InboundMessageDto(
                "govflow-consultoria",
                "MSG_ALREADY_EXISTS_999",
                "5583999999999",
                "Prefeito",
                "DOCUMENT",
                null, null, null, null, null, "{}", false
        );

        when(parser.supports(eq("EVOLUTION"), any(JsonNode.class))).thenReturn(true);
        when(parser.parse(any(JsonNode.class))).thenReturn(Optional.of(dto));
        when(mensagemRepository.existsByExternalMessageId("MSG_ALREADY_EXISTS_999")).thenReturn(true);

        WebhookResponseDto response = processor.processWebhook(rootNode);

        assertEquals("ALREADY_EXISTS", response.status());
        assertEquals("MSG_ALREADY_EXISTS_999", response.externalMessageId());

        // NUNCA deve tentar salvar novamente nem publicar evento
        verify(mensagemRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
