package br.com.govflow.core.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DiffRevisaoTest {

    @Test
    @DisplayName("Deve identificar nenhuma alteração quando valores são idênticos")
    void deveRetornarDiffVazioQuandoValoresIdenticos() {
        ExtracaoSugerida extracao = new ExtracaoSugerida(
                TipoDocumentoHabil.NOTA_FISCAL_SERVICOS,
                "123",
                "1",
                null,
                LocalDate.of(2026, 5, 10),
                "12345678000190",
                "Empresa A",
                "Serviços",
                null,
                new BigDecimal("1000.00"),
                BigDecimal.ZERO,
                new BigDecimal("1000.00"),
                List.of(),
                0.95,
                true,
                List.of()
        );

        DadosRevisaoAnalista revisao = new DadosRevisaoAnalista(
                TipoDocumentoHabil.NOTA_FISCAL_SERVICOS,
                "123",
                "1",
                null,
                LocalDate.of(2026, 5, 10),
                "12345678000190",
                "Empresa A",
                "Serviços",
                null,
                new BigDecimal("1000.00"),
                BigDecimal.ZERO,
                new BigDecimal("1000.00"),
                List.of(),
                "OK"
        );

        DiffRevisao diff = DiffRevisao.comparar(extracao, revisao);

        assertFalse(diff.temAlteracoes());
        assertTrue(diff.alteracoes().isEmpty());
    }

    @Test
    @DisplayName("Deve registrar múltiplos campos alterados no diff")
    void deveRegistrarMultiplosCamposAlterados() {
        ExtracaoSugerida extracao = new ExtracaoSugerida(
                TipoDocumentoHabil.NOTA_FISCAL_SERVICOS,
                "123",
                "1",
                null,
                LocalDate.of(2026, 5, 10),
                "12345678000190",
                "Empresa A",
                "Serviços",
                null,
                new BigDecimal("1000.00"),
                BigDecimal.ZERO,
                new BigDecimal("1000.00"),
                List.of(),
                0.95,
                true,
                List.of()
        );

        DadosRevisaoAnalista revisao = new DadosRevisaoAnalista(
                TipoDocumentoHabil.NOTA_FISCAL_MERCADORIAS,
                "124",
                "2",
                null,
                LocalDate.of(2026, 5, 12),
                "98765432000100",
                "Empresa B",
                "Fornecimento",
                null,
                new BigDecimal("1200.00"),
                new BigDecimal("100.00"),
                new BigDecimal("1100.00"),
                List.of(),
                "Corrigido"
        );

        DiffRevisao diff = DiffRevisao.comparar(extracao, revisao);

        assertTrue(diff.temAlteracoes());
        assertEquals("NOTA_FISCAL_SERVICOS", diff.alteracoes().get("tipoDocumento").de());
        assertEquals("NOTA_FISCAL_MERCADORIAS", diff.alteracoes().get("tipoDocumento").para());
        assertEquals("123", diff.alteracoes().get("numeroDocumento").de());
        assertEquals("124", diff.alteracoes().get("numeroDocumento").para());
        assertEquals("1000.00", diff.alteracoes().get("valorBruto").de());
        assertEquals("1200.00", diff.alteracoes().get("valorBruto").para());
    }

    @Test
    @DisplayName("Deve registrar alterações em retenções tributárias no diff")
    void deveRegistrarAlteracoesEmRetencoesTributarias() {
        ExtracaoSugerida extracao = new ExtracaoSugerida(
                TipoDocumentoHabil.NOTA_FISCAL_SERVICOS,
                "123",
                "1",
                null,
                LocalDate.of(2026, 5, 10),
                "12345678000190",
                "Empresa A",
                "Serviços",
                null,
                new BigDecimal("1000.00"),
                new BigDecimal("110.00"),
                new BigDecimal("890.00"),
                List.of(new RetencaoTributaria(TipoRetencao.INSS, new BigDecimal("11.0"), new BigDecimal("110.00"), 0.99, null)),
                0.95,
                true,
                List.of()
        );

        // Analista adiciona retenção de ISS
        DadosRevisaoAnalista revisao = new DadosRevisaoAnalista(
                TipoDocumentoHabil.NOTA_FISCAL_SERVICOS,
                "123",
                "1",
                null,
                LocalDate.of(2026, 5, 10),
                "12345678000190",
                "Empresa A",
                "Serviços",
                null,
                new BigDecimal("1000.00"),
                new BigDecimal("160.00"),
                new BigDecimal("840.00"),
                List.of(
                        new RetencaoTributaria(TipoRetencao.INSS, new BigDecimal("11.0"), new BigDecimal("110.00"), 0.99, null),
                        new RetencaoTributaria(TipoRetencao.ISS, new BigDecimal("5.0"), new BigDecimal("50.00"), 1.0, null)
                ),
                "Adicionado ISS retido na fonte"
        );

        DiffRevisao diff = DiffRevisao.comparar(extracao, revisao);

        assertTrue(diff.temAlteracoes());
        assertTrue(diff.alteracoes().containsKey("retencoes"));
        assertNotEquals(diff.alteracoes().get("retencoes").de(), diff.alteracoes().get("retencoes").para());
    }
}
