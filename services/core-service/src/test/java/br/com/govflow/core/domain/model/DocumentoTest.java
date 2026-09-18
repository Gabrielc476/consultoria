package br.com.govflow.core.domain.model;

import br.com.govflow.core.domain.exception.CamposObrigatoriosAusentesException;
import br.com.govflow.core.domain.exception.DocumentoEstadoInvalidoException;
import br.com.govflow.core.domain.exception.InconsistenciaMatematicaException;
import br.com.govflow.core.domain.exception.JustificativaObrigatoriaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DocumentoTest {

    private UUID tenantId;
    private UUID analistaId;
    private Documento documento;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        analistaId = UUID.randomUUID();
        documento = Documento.criarRecebido(
                UUID.randomUUID(),
                tenantId,
                UUID.randomUUID(),
                null,
                "govflow-bucket",
                "docs/2026/05/nf123.pdf",
                "nf123.pdf",
                "application/pdf",
                1048576L
        );
    }

    @Test
    @DisplayName("Deve criar documento com status RECEBIDO inicialmente")
    void deveCriarDocumentoComStatusRecebido() {
        assertEquals(StatusDocumento.RECEBIDO, documento.getStatus());
        assertNull(documento.getExtracaoSugerida());
        assertTrue(documento.getBoundingBoxes().isEmpty());
    }

    @Test
    @DisplayName("Deve registrar extração da IA e transicionar para EM_CONFERENCIA")
    void deveRegistrarExtracaoIATransicionandoParaEmConferencia() {
        ExtracaoSugerida extracao = criarExtracaoValida();
        BoundingBoxesData boxes = BoundingBoxesData.of(Map.of("numeroDocumento", new BoundingBox(0.1, 0.2, 0.3, 0.4)));

        documento.registrarExtracaoIA(extracao, boxes);

        assertEquals(StatusDocumento.EM_CONFERENCIA, documento.getStatus());
        assertNotNull(documento.getExtracaoSugerida());
        assertEquals("000123", documento.getExtracaoSugerida().numeroDocumento());
        assertEquals(1, documento.getBoundingBoxes().size());
        assertTrue(documento.getBoundingBoxesData().contem("numeroDocumento"));
    }

    @Test
    @DisplayName("Deve bloquear nova extração de IA se documento já estiver aprovado")
    void deveBloquearExtracaoSeDocumentoJaAprovado() {
        documento.registrarExtracaoIA(criarExtracaoValida(), Map.of());
        DadosRevisaoAnalista revisao = criarRevisaoValida();
        documento.aprovar(analistaId, revisao, "Aprovado sem ressalvas");

        assertThrows(DocumentoEstadoInvalidoException.class, () ->
                documento.registrarExtracaoIA(criarExtracaoValida(), Map.of())
        );
    }

    @Test
    @DisplayName("Deve aprovar documento em conferência calculando diff e gerando auditoria")
    void deveAprovarDocumentoComSucesso() {
        documento.registrarExtracaoIA(criarExtracaoValida(), Map.of());

        // Analista corrige o número do documento de "000123" para "000124"
        DadosRevisaoAnalista revisao = new DadosRevisaoAnalista(
                TipoDocumentoHabil.NOTA_FISCAL_SERVICOS,
                "000124",
                "1",
                null,
                LocalDate.of(2026, 5, 10),
                "12.345.678/0001-95",
                "Empreiteira Construir Ltda",
                "Serviços de pavimentação",
                null,
                new BigDecimal("10000.00"),
                new BigDecimal("1100.00"),
                new BigDecimal("8900.00"),
                List.of(new RetencaoTributaria(TipoRetencao.INSS, new BigDecimal("11.0"), new BigDecimal("1100.00"), 0.99, null)),
                "Aprovado com correção no número da nota"
        );

        AuditoriaRevisao auditoria = documento.aprovar(analistaId, revisao, "Nota conferida");

        assertEquals(StatusDocumento.PRONTO_PARA_TRANSFEREGOV, documento.getStatus());
        assertNotNull(auditoria);
        assertEquals(AcaoAuditoria.APROVACAO, auditoria.getAcao());
        assertEquals(analistaId, auditoria.getAnalistaId());
        assertTrue(auditoria.getDiff().temAlteracoes());
        assertEquals("000123", auditoria.getDiff().alteracoes().get("numeroDocumento").de());
        assertEquals("000124", auditoria.getDiff().alteracoes().get("numeroDocumento").para());
    }

    @Test
    @DisplayName("Deve lançar exceção se tentar aprovar documento fora do estado EM_CONFERENCIA")
    void deveBloquearAprovacaoSeNaoEstiverEmConferencia() {
        DadosRevisaoAnalista revisao = criarRevisaoValida();

        // Documento ainda está em RECEBIDO (não recebeu extração de IA)
        assertThrows(DocumentoEstadoInvalidoException.class, () ->
                documento.aprovar(analistaId, revisao, "Aprovação prematura")
        );
    }

    @Test
    @DisplayName("Deve validar campos obrigatórios na aprovação")
    void deveValidarCamposObrigatoriosNaAprovacao() {
        documento.registrarExtracaoIA(criarExtracaoValida(), Map.of());

        // Revisão sem número e com valor bruto zerado
        DadosRevisaoAnalista revisaoInvalida = new DadosRevisaoAnalista(
                TipoDocumentoHabil.NOTA_FISCAL_SERVICOS,
                null,
                "1",
                null,
                LocalDate.of(2026, 5, 10),
                null,
                "Empreiteira Construir Ltda",
                "Serviços",
                null,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                List.of(),
                "Incompleto"
        );

        CamposObrigatoriosAusentesException ex = assertThrows(CamposObrigatoriosAusentesException.class, () ->
                documento.aprovar(analistaId, revisaoInvalida, "Tentativa inválida")
        );

        assertTrue(ex.getCamposFaltantes().contains("numeroDocumento"));
        assertTrue(ex.getCamposFaltantes().contains("cnpjCredor"));
    }

    @Test
    @DisplayName("Deve rejeitar aprovação se CNPJ for matematicamente inválido")
    void deveRejeitarAprovacaoSeCnpjInvalido() {
        documento.registrarExtracaoIA(criarExtracaoValida(), Map.of());

        DadosRevisaoAnalista revisaoCnpjInvalido = new DadosRevisaoAnalista(
                TipoDocumentoHabil.NOTA_FISCAL_SERVICOS,
                "000123",
                "1",
                null,
                LocalDate.of(2026, 5, 10),
                "12.345.678/0001-90", // DV incorreto
                "Empreiteira Construir Ltda",
                "Serviços",
                null,
                new BigDecimal("10000.00"),
                BigDecimal.ZERO,
                new BigDecimal("10000.00"),
                List.of(),
                "CNPJ com DV inválido"
        );

        CamposObrigatoriosAusentesException ex = assertThrows(CamposObrigatoriosAusentesException.class, () ->
                documento.aprovar(analistaId, revisaoCnpjInvalido, "Tentativa com CNPJ inválido")
        );

        assertTrue(ex.getCamposFaltantes().stream().anyMatch(c -> c.contains("cnpjCredor")));
    }

    @Test
    @DisplayName("Deve lançar InconsistenciaMatematicaException com tolerância zero (diferença de R$ 0.01)")
    void deveRejeitarDiferencaComToleranciaZero() {
        documento.registrarExtracaoIA(criarExtracaoValida(), Map.of());

        // Bruto = 10000.00, Deduções = 1100.00, Líquido = 8899.99 (diferença de 1 centavo)
        DadosRevisaoAnalista revisaoCom1CentavoDiferenca = new DadosRevisaoAnalista(
                TipoDocumentoHabil.NOTA_FISCAL_SERVICOS,
                "000123",
                "1",
                null,
                LocalDate.of(2026, 5, 10),
                "12.345.678/0001-95",
                "Empreiteira Construir Ltda",
                "Serviços",
                null,
                new BigDecimal("10000.00"),
                new BigDecimal("1100.00"),
                new BigDecimal("8899.99"),
                List.of(new RetencaoTributaria(TipoRetencao.INSS, new BigDecimal("11.0"), new BigDecimal("1100.00"), 0.99, null)),
                "Divergência de 1 centavo"
        );

        InconsistenciaMatematicaException ex = assertThrows(InconsistenciaMatematicaException.class, () ->
                documento.aprovar(analistaId, revisaoCom1CentavoDiferenca, "Valores com 1 centavo")
        );

        assertEquals(new BigDecimal("0.01"), ex.getDiferenca());
    }

    @Test
    @DisplayName("Deve rejeitar se a soma das retenções divergir do valor total de deduções")
    void deveRejeitarSeSomaRetencoesDivergirDoTotalDeducoes() {
        documento.registrarExtracaoIA(criarExtracaoValida(), Map.of());

        // INSS retido = 1100.00, mas valorTotalDeducoes = 1200.00
        DadosRevisaoAnalista revisaoComRetencoesDivergentes = new DadosRevisaoAnalista(
                TipoDocumentoHabil.NOTA_FISCAL_SERVICOS,
                "000123",
                "1",
                null,
                LocalDate.of(2026, 5, 10),
                "12.345.678/0001-95",
                "Empreiteira Construir Ltda",
                "Serviços",
                null,
                new BigDecimal("10000.00"),
                new BigDecimal("1200.00"),
                new BigDecimal("8800.00"),
                List.of(new RetencaoTributaria(TipoRetencao.INSS, new BigDecimal("11.0"), new BigDecimal("1100.00"), 0.99, null)),
                "Deduções não batem com retenções"
        );

        assertThrows(InconsistenciaMatematicaException.class, () ->
                documento.aprovar(analistaId, revisaoComRetencoesDivergentes, "Tentativa com retenções divergentes")
        );
    }

    @Test
    @DisplayName("Deve aprovar quando as contas fecham com precisão matemática exata")
    void deveAprovarComPrecisaoMatematicaExata() {
        documento.registrarExtracaoIA(criarExtracaoValida(), Map.of());

        DadosRevisaoAnalista revisaoExata = new DadosRevisaoAnalista(
                TipoDocumentoHabil.NOTA_FISCAL_SERVICOS,
                "000123",
                "1",
                null,
                LocalDate.of(2026, 5, 10),
                "12.345.678/0001-95",
                "Empreiteira Construir Ltda",
                "Serviços",
                null,
                new BigDecimal("10000.00"),
                new BigDecimal("1100.00"),
                new BigDecimal("8900.00"),
                List.of(new RetencaoTributaria(TipoRetencao.INSS, new BigDecimal("11.0"), new BigDecimal("1100.00"), 0.99, null)),
                "Valores exatos"
        );

        assertDoesNotThrow(() -> documento.aprovar(analistaId, revisaoExata, "Precisão exata"));
        assertEquals(StatusDocumento.PRONTO_PARA_TRANSFEREGOV, documento.getStatus());
    }

    @Test
    @DisplayName("Deve rejeitar documento em conferência com justificativa obrigatória")
    void deveRejeitarDocumentoComSucesso() {
        documento.registrarExtracaoIA(criarExtracaoValida(), Map.of());

        AuditoriaRevisao auditoria = documento.rejeitar(analistaId, "Documento ilegível e CNPJ divergente do contrato");

        assertEquals(StatusDocumento.REJEITADO, documento.getStatus());
        assertEquals("Documento ilegível e CNPJ divergente do contrato", documento.getMotivoRejeicao());
        assertEquals(AcaoAuditoria.REJEICAO, auditoria.getAcao());
    }

    @Test
    @DisplayName("Deve lançar JustificativaObrigatoriaException ao rejeitar com motivo vazio")
    void deveLancarExcecaoAoRejeitarSemMotivo() {
        documento.registrarExtracaoIA(criarExtracaoValida(), Map.of());

        assertThrows(JustificativaObrigatoriaException.class, () ->
                documento.rejeitar(analistaId, "   ")
        );
    }

    private ExtracaoSugerida criarExtracaoValida() {
        return new ExtracaoSugerida(
                TipoDocumentoHabil.NOTA_FISCAL_SERVICOS,
                "000123",
                "1",
                "35240512345678000195550010000001231000001234",
                LocalDate.of(2026, 5, 10),
                "12.345.678/0001-95",
                "Empreiteira Construir Ltda",
                "Serviços de pavimentação",
                "EMP-001",
                new BigDecimal("10000.00"),
                new BigDecimal("1100.00"),
                new BigDecimal("8900.00"),
                List.of(new RetencaoTributaria(TipoRetencao.INSS, new BigDecimal("11.0"), new BigDecimal("1100.00"), 0.99, null)),
                0.985,
                true,
                List.of()
        );
    }

    private DadosRevisaoAnalista criarRevisaoValida() {
        return new DadosRevisaoAnalista(
                TipoDocumentoHabil.NOTA_FISCAL_SERVICOS,
                "000123",
                "1",
                null,
                LocalDate.of(2026, 5, 10),
                "12.345.678/0001-95",
                "Empreiteira Construir Ltda",
                "Serviços de pavimentação",
                null,
                new BigDecimal("10000.00"),
                new BigDecimal("1100.00"),
                new BigDecimal("8900.00"),
                List.of(new RetencaoTributaria(TipoRetencao.INSS, new BigDecimal("11.0"), new BigDecimal("1100.00"), 0.99, null)),
                "Aprovado conforme contrato"
        );
    }
}
