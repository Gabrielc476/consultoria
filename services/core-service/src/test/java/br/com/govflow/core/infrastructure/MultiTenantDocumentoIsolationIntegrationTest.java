package br.com.govflow.core.infrastructure;

import br.com.govflow.core.application.port.in.AprovarDocumentoUseCase;
import br.com.govflow.core.application.port.in.ConsultarDocumentoUseCase;
import br.com.govflow.core.application.port.in.ProcessarDocumentoExtraidoUseCase;
import br.com.govflow.core.application.port.out.DocumentoRepositoryPort;
import br.com.govflow.core.domain.model.*;
import br.com.govflow.core.infrastructure.adapter.out.persistence.repository.SpringDataDocumentoRepository;
import br.com.govflow.core.infrastructure.interceptor.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class MultiTenantDocumentoIsolationIntegrationTest {

    @Autowired
    private ProcessarDocumentoExtraidoUseCase processarUseCase;

    @Autowired
    private ConsultarDocumentoUseCase consultarUseCase;

    @Autowired
    private AprovarDocumentoUseCase aprovarUseCase;

    @Autowired
    private DocumentoRepositoryPort documentoRepository;

    @Autowired
    private SpringDataDocumentoRepository repository;

    @org.springframework.boot.test.mock.mockito.MockBean
    private br.com.govflow.core.application.port.out.DocumentoEventPublisherPort eventPublisher;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        repository.deleteAll();
    }

    @Test
    @DisplayName("Garante isolamento multi-tenant (@TenantId): Tenant A jamais acessa documentos do Tenant B")
    void deveGarantirIsolamentoTotalEntreTenantsConcorrentes() {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();

        // 1. Cadastra 2 Documentos para o Tenant A
        TenantContext.setCurrentTenant(tenantA);

        UUID docA1Id = UUID.randomUUID();
        UUID docA2Id = UUID.randomUUID();

        processarUseCase.processar(new ProcessarDocumentoExtraidoUseCase.ProcessarDocumentoExtraidoCommand(
                tenantA,
                docA1Id,
                null,
                null,
                "bucketA",
                "docs/nfA1.pdf",
                "nfA1.pdf",
                "application/pdf",
                1000L,
                criarExtracao("001"),
                Map.of("numeroDocumento", new BoundingBox(0.1, 0.1, 0.2, 0.2))
        ));

        processarUseCase.processar(new ProcessarDocumentoExtraidoUseCase.ProcessarDocumentoExtraidoCommand(
                tenantA,
                docA2Id,
                null,
                null,
                "bucketA",
                "docs/nfA2.pdf",
                "nfA2.pdf",
                "application/pdf",
                1500L,
                criarExtracao("002"),
                Map.of()
        ));

        // 2. Muda para o Tenant B e cadastra 1 Documento
        TenantContext.setCurrentTenant(tenantB);

        UUID docB1Id = UUID.randomUUID();
        processarUseCase.processar(new ProcessarDocumentoExtraidoUseCase.ProcessarDocumentoExtraidoCommand(
                tenantB,
                docB1Id,
                null,
                null,
                "bucketB",
                "docs/nfB1.pdf",
                "nfB1.pdf",
                "application/pdf",
                2000L,
                criarExtracao("101"),
                Map.of()
        ));

        // 3. Validação do Tenant B: deve enxergar APENAS docB1
        List<Documento> docsTenantB = consultarUseCase.listar(0, 10, null);
        assertEquals(1, docsTenantB.size());
        assertEquals(docB1Id, docsTenantB.get(0).getId());
        assertEquals(1, consultarUseCase.contar(null), "Contagem total do Tenant B deve ser exatamente 1");
        assertEquals(1, consultarUseCase.contar(StatusDocumento.EM_CONFERENCIA), "Contagem por status do Tenant B deve ser exatamente 1");

        // Tenant B NÃO pode achar documentos de Tenant A por ID
        Optional<Documento> buscaCruzadaA1 = consultarUseCase.buscarPorId(docA1Id);
        Optional<Documento> buscaCruzadaA2 = consultarUseCase.buscarPorId(docA2Id);
        assertTrue(buscaCruzadaA1.isEmpty(), "Tenant B não pode acessar Documento A1 do Tenant A");
        assertTrue(buscaCruzadaA2.isEmpty(), "Tenant B não pode acessar Documento A2 do Tenant A");

        // 4. Muda contexto de volta para o Tenant A
        TenantContext.setCurrentTenant(tenantA);

        List<Documento> docsTenantA = consultarUseCase.listar(0, 10, null);
        assertEquals(2, docsTenantA.size());
        assertEquals(2, consultarUseCase.contar(null), "Contagem total do Tenant A deve ser exatamente 2");
        assertEquals(2, consultarUseCase.contar(StatusDocumento.EM_CONFERENCIA), "Contagem por status do Tenant A deve ser exatamente 2");
        assertTrue(docsTenantA.stream().anyMatch(d -> d.getId().equals(docA1Id)));
        assertTrue(docsTenantA.stream().anyMatch(d -> d.getId().equals(docA2Id)));
        assertFalse(docsTenantA.stream().anyMatch(d -> d.getId().equals(docB1Id)),
                "Documento do Tenant B jamais pode aparecer na listagem do Tenant A");

        // Tenant A NÃO pode achar documento do Tenant B por ID
        Optional<Documento> buscaCruzadaB1 = consultarUseCase.buscarPorId(docB1Id);
        assertTrue(buscaCruzadaB1.isEmpty(), "Tenant A não pode acessar Documento B1 do Tenant B");
    }

    @Test
    @DisplayName("Garante fidelidade de retenções revisadas ao salvar e recarregar do banco de dados")
    void devePreservarRetencoesRevisadasAoRecarregarDoBanco() {
        UUID tenantId = UUID.randomUUID();
        UUID analistaId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        TenantContext.setCurrentTenant(tenantId);

        // 1. Processa documento vindo da IA (sem retenções ou com sugestão preliminar)
        processarUseCase.processar(new ProcessarDocumentoExtraidoUseCase.ProcessarDocumentoExtraidoCommand(
                tenantId,
                docId,
                null,
                null,
                "bucket",
                "docs/nf.pdf",
                "nf.pdf",
                "application/pdf",
                1000L,
                criarExtracao("999"),
                Map.of()
        ));

        // 2. Analista aprova adicionando retenções tributárias específicas (INSS e ISS)
        List<RetencaoTributaria> retencoesRevisadas = List.of(
                new RetencaoTributaria(TipoRetencao.INSS, new BigDecimal("11.0"), new BigDecimal("550.00"), 1.0, null),
                new RetencaoTributaria(TipoRetencao.ISS, new BigDecimal("5.0"), new BigDecimal("250.00"), 1.0, null)
        );

        DadosRevisaoAnalista revisao = new DadosRevisaoAnalista(
                TipoDocumentoHabil.NOTA_FISCAL_SERVICOS,
                "999",
                "1",
                null,
                LocalDate.of(2026, 5, 10),
                "12345678000195",
                "Empresa Fornecedora",
                "Prestação de Serviços",
                null,
                new BigDecimal("5000.00"),
                new BigDecimal("800.00"),
                new BigDecimal("4200.00"),
                retencoesRevisadas,
                "Aprovado com deduções tributárias"
        );

        aprovarUseCase.aprovar(new AprovarDocumentoUseCase.AprovarDocumentoCommand(
                docId,
                analistaId,
                revisao,
                "Aprovado"
        ));

        // 3. Recarrega o documento do banco em nova consulta
        Documento documentoRecarregado = documentoRepository.buscarPorId(docId)
                .orElseThrow(() -> new AssertionError("Documento deveria existir"));

        assertEquals(StatusDocumento.PRONTO_PARA_TRANSFEREGOV, documentoRecarregado.getStatus());
        assertNotNull(documentoRecarregado.getDadosRevisao(), "Dados de revisão devem estar preenchidos");

        List<RetencaoTributaria> retencoesRecarregadas = documentoRecarregado.getDadosRevisao().retencoes();
        assertEquals(2, retencoesRecarregadas.size(), "Deve conter exatamente as 2 retenções revisadas pelo analista");
        assertTrue(retencoesRecarregadas.stream().anyMatch(r -> r.tipo() == TipoRetencao.INSS && r.valor().compareTo(new BigDecimal("550.00")) == 0));
        assertTrue(retencoesRecarregadas.stream().anyMatch(r -> r.tipo() == TipoRetencao.ISS && r.valor().compareTo(new BigDecimal("250.00")) == 0));
    }

    private ExtracaoSugerida criarExtracao(String numero) {
        return new ExtracaoSugerida(
                TipoDocumentoHabil.NOTA_FISCAL_SERVICOS,
                numero,
                "1",
                null,
                LocalDate.of(2026, 5, 10),
                "12345678000195",
                "Empresa Fornecedora",
                "Prestação de Serviços",
                null,
                new BigDecimal("5000.00"),
                BigDecimal.ZERO,
                new BigDecimal("5000.00"),
                List.of(),
                0.99,
                true,
                List.of()
        );
    }
}
