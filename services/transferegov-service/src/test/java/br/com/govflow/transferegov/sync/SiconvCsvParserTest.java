package br.com.govflow.transferegov.sync;

import br.com.govflow.transferegov.domain.model.ConvenioSincronizado;
import br.com.govflow.transferegov.domain.model.ProponenteInfo;
import br.com.govflow.transferegov.domain.model.PropostaInfo;
import br.com.govflow.transferegov.sync.mock.MockSiconvArchiveGenerator;
import br.com.govflow.transferegov.sync.pipeline.SiconvCsvParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes Unitários do Parser de Streaming SICONV (SiconvCsvParser)")
class SiconvCsvParserTest {

    private SiconvCsvParser parser;

    @BeforeEach
    void setUp() {
        parser = new SiconvCsvParser();
    }

    @Test
    @DisplayName("Deve ler sentinela de data de carga a partir do arquivo ZIP em memória")
    void deveLerSentinelaDataCarga() throws IOException {
        byte[] zipBytes = MockSiconvArchiveGenerator.generateSentinelaZip("23/09/2026 06:36:22");

        String sentinela = parser.readSentinelaDataCarga(new ByteArrayInputStream(zipBytes));

        assertEquals("23/09/2026 06:36:22", sentinela);
    }

    @Test
    @DisplayName("Deve filtrar proponentes em voo por UF='PB' e descartar outros estados sem reter na memória")
    void deveFiltrarProponentesPorUf() throws IOException {
        byte[] zipBytes = MockSiconvArchiveGenerator.generateProponentesZip();

        Map<String, ProponenteInfo> proponentes = parser.streamProponentes(
                new ByteArrayInputStream(zipBytes),
                "PB",
                Collections.emptySet()
        );

        assertEquals(2, proponentes.size(), "Deve conter apenas os 2 proponentes da Paraíba");
        assertTrue(proponentes.containsKey("1001"), "Deve conter Massaranduba");
        assertTrue(proponentes.containsKey("1002"), "Deve conter João Pessoa");
        assertFalse(proponentes.containsKey("9999"), "Campinas/SP deve ser descartada");
    }

    @Test
    @DisplayName("Deve filtrar propostas associadas exclusivamente aos proponentes retidos")
    void deveFiltrarPropostasDeProponentesRetidos() throws IOException {
        byte[] zipBytes = MockSiconvArchiveGenerator.generatePropostasZip();
        Set<String> proponentesPb = Set.of("1001", "1002");

        Map<String, PropostaInfo> propostas = parser.streamPropostas(
                new ByteArrayInputStream(zipBytes),
                proponentesPb
        );

        assertEquals(2, propostas.size());
        assertTrue(propostas.containsKey("5001"));
        assertTrue(propostas.containsKey("5002"));
        assertFalse(propostas.containsKey("8888"), "Proposta de SP deve ser descartada");
    }

    @Test
    @DisplayName("Deve realizar streaming de convênios cruzando dados com propostas e proponentes")
    void deveRealizarStreamingConvenios() throws IOException {
        byte[] zipBytes = MockSiconvArchiveGenerator.generateConveniosZip();

        Map<String, ProponenteInfo> proponentes = Map.of(
                "1001", new ProponenteInfo("1001", "08923456000112", "MASSARANDUBA", "Massaranduba", "PB")
        );
        Map<String, PropostaInfo> propostas = Map.of(
                "5001", new PropostaInfo("5001", "1001", "Pavimentacao")
        );

        List<ConvenioSincronizado> capturados = new ArrayList<>();
        parser.streamConvenios(
                new ByteArrayInputStream(zipBytes),
                proponentes,
                propostas,
                "23/09/2026",
                (conv, row) -> capturados.add(conv)
        );

        // Das 4 linhas do arquivo, 2 pertencem à proposta 5001 (912345 e 912347)
        assertEquals(2, capturados.size());
        assertEquals("912345", capturados.get(0).nrConvenio());
        assertEquals(new BigDecimal("500000.00"), capturados.get(0).valorGlobal());
        assertEquals("08923456000112", capturados.get(0).cnpjProponente());
    }

    @Test
    @DisplayName("Deve converter formatos monetários brasileiros e ISO com tolerância e segurança")
    void deveConverterMoedaCorretamente() {
        assertEquals(new BigDecimal("1250000.50"), SiconvCsvParser.parseMoney("1.250.000,50"));
        assertEquals(new BigDecimal("85400.00"), SiconvCsvParser.parseMoney("85400.00"));
        assertEquals(new BigDecimal("0.00"), SiconvCsvParser.parseMoney("0,00"));
        assertEquals(BigDecimal.ZERO, SiconvCsvParser.parseMoney(""));
        assertEquals(BigDecimal.ZERO, SiconvCsvParser.parseMoney(null));
    }

    @Test
    @DisplayName("Deve converter datas nos padrões brasileiro (dd/MM/yyyy) e ISO (yyyy-MM-dd)")
    void deveConverterDatasCorretamente() {
        assertEquals(LocalDate.of(2026, 12, 31), SiconvCsvParser.parseDate("31/12/2026"));
        assertEquals(LocalDate.of(2024, 1, 15), SiconvCsvParser.parseDate("2024-01-15"));
        assertNull(SiconvCsvParser.parseDate(""));
        assertNull(SiconvCsvParser.parseDate(null));
        assertNull(SiconvCsvParser.parseDate("data_invalida"));
    }
}
