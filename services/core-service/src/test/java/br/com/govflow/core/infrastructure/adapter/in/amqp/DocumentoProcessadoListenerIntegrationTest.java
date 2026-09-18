package br.com.govflow.core.infrastructure.adapter.in.amqp;

import br.com.govflow.core.application.port.in.ConsultarDocumentoUseCase;
import br.com.govflow.core.domain.model.Documento;
import br.com.govflow.core.domain.model.StatusDocumento;
import br.com.govflow.core.infrastructure.adapter.in.amqp.dto.DocumentoExtraidoEventDto;
import br.com.govflow.core.infrastructure.adapter.in.amqp.dto.DocumentoExtraidoPayloadDto;
import br.com.govflow.core.infrastructure.adapter.out.persistence.repository.SpringDataDocumentoRepository;
import br.com.govflow.core.infrastructure.interceptor.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class DocumentoProcessadoListenerIntegrationTest {

    @Autowired
    private DocumentoProcessadoListener listener;

    @Autowired
    private ConsultarDocumentoUseCase consultarUseCase;

    @Autowired
    private SpringDataDocumentoRepository repository;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        repository.deleteAll();
    }

    @Test
    @DisplayName("Deve consumir DocumentoExtraidoEventDto, persistir Documento em EM_CONFERENCIA e limpar TenantContext")
    void deveConsumirEventoComSucesso() {
        UUID tenantId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();

        Map<String, Object> extracao = Map.of(
                "tipo_documento", Map.of("valor", "NOTA_FISCAL_SERVICOS", "confianca", 0.99),
                "numero_documento", Map.of("valor", "009988", "confianca", 0.99),
                "data_emissao", Map.of("valor", "2026-05-15", "confianca", 0.99),
                "cnpj_credor", Map.of("valor", "98765432000100", "confianca", 0.99),
                "razao_social_credor", Map.of("valor", "Fornecedor Asfalto PB", "confianca", 0.99),
                "descricao_servico", Map.of("valor", "Recapeamento", "confianca", 0.99),
                "valor_bruto", Map.of("valor", "25000.00", "confianca", 0.99),
                "valor_liquido", Map.of("valor", "22500.00", "confianca", 0.99),
                "retencoes", List.of(
                        Map.of("tipo", "INSS", "aliquota", 10.0, "valor", 2500.0, "confianca", 0.99)
                )
        );

        Map<String, Object> validacao = Map.of(
                "consistente", true,
                "totalRetencoes", 2500.0,
                "valorBruto", 25000.0,
                "valorLiquidoInformado", 22500.0
        );

        Map<String, Object> boxes = Map.of(
                "numero_documento", List.of(0.12, 0.45, 0.15, 0.60)
        );

        DocumentoExtraidoPayloadDto payload = new DocumentoExtraidoPayloadDto(
                docId,
                "govflow-bucket",
                "uploads/doc9988.pdf",
                null,
                null,
                "doc9988.pdf",
                "application/pdf",
                204800L,
                extracao,
                validacao,
                0.99,
                boxes
        );

        DocumentoExtraidoEventDto event = new DocumentoExtraidoEventDto(
                UUID.randomUUID(),
                "DocumentoExtraidoEvent",
                "1.0.0",
                Instant.now(),
                tenantId,
                docId,
                payload
        );

        listener.onDocumentoExtraido(event);

        // TenantContext deve ter sido limpo no finally
        assertNull(TenantContext.getCurrentTenant(), "TenantContext deve ser limpo após o listener");

        // Consulta autenticado no tenant para verificar persistência
        TenantContext.setCurrentTenant(tenantId);
        try {
            Optional<Documento> optDoc = consultarUseCase.buscarPorId(docId);
            assertTrue(optDoc.isPresent(), "Documento deve ter sido persistido");

            Documento doc = optDoc.get();
            assertEquals(StatusDocumento.EM_CONFERENCIA, doc.getStatus());
            assertNotNull(doc.getExtracaoSugerida());
            assertEquals("009988", doc.getExtracaoSugerida().numeroDocumento());
            assertEquals("98765432000100", doc.getExtracaoSugerida().cnpjCredor());
            assertEquals(new java.math.BigDecimal("25000.00"), doc.getExtracaoSugerida().valorBruto());
            assertNotNull(doc.getExtracaoSugerida().scoresConfiancaCampos());
            assertEquals(0.99, doc.getExtracaoSugerida().scoresConfiancaCampos().get("valorBruto"));
            assertEquals(1, doc.getBoundingBoxes().size());
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    @DisplayName("Deve lançar AmqpRejectAndDontRequeueException quando tenantId for nulo para que o RabbitMQ envie à DLQ")
    void deveLancarExcecaoParaDLQQuandoTenantIdNulo() {
        DocumentoExtraidoEventDto eventSemTenant = new DocumentoExtraidoEventDto(
                UUID.randomUUID(),
                "DocumentoExtraidoEvent",
                "1.0.0",
                Instant.now(),
                null,
                UUID.randomUUID(),
                new DocumentoExtraidoPayloadDto(
                        UUID.randomUUID(), "bucket", "key", null, null, "doc.pdf", "application/pdf", 100L,
                        Map.of(), Map.of(), 0.9, Map.of()
                )
        );

        assertThrows(org.springframework.amqp.AmqpRejectAndDontRequeueException.class, () -> {
            listener.onDocumentoExtraido(eventSemTenant);
        });
    }

    @Test
    @DisplayName("Deve lançar AmqpRejectAndDontRequeueException quando payload for nulo para envio à DLQ")
    void deveLancarExcecaoParaDLQQuandoPayloadNulo() {
        DocumentoExtraidoEventDto eventSemPayload = new DocumentoExtraidoEventDto(
                UUID.randomUUID(),
                "DocumentoExtraidoEvent",
                "1.0.0",
                Instant.now(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                null
        );

        assertThrows(org.springframework.amqp.AmqpRejectAndDontRequeueException.class, () -> {
            listener.onDocumentoExtraido(eventSemPayload);
        });
    }
}
