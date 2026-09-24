package br.com.govflow.transferegov.sync.rest;

import br.com.govflow.transferegov.persistence.entity.SincronizacaoConvenioEntity;
import br.com.govflow.transferegov.persistence.entity.SincronizacaoLogEntity;
import br.com.govflow.transferegov.persistence.repository.SincronizacaoAnomaliaRepository;
import br.com.govflow.transferegov.persistence.repository.SincronizacaoConvenioRepository;
import br.com.govflow.transferegov.persistence.repository.SincronizacaoLogRepository;
import br.com.govflow.transferegov.sync.client.SiconvStreamingClient;
import br.com.govflow.transferegov.sync.mock.MockSiconvArchiveGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Testes dos Endpoints REST de Sincronização e Consulta de Convênios (CQRS)")
class SincronizacaoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SincronizacaoConvenioRepository convenioRepository;

    @Autowired
    private SincronizacaoLogRepository logRepository;

    @Autowired
    private SincronizacaoAnomaliaRepository anomaliaRepository;

    @MockBean
    private SiconvStreamingClient streamingClient;

    @BeforeEach
    void setUp() throws IOException {
        convenioRepository.deleteAll();
        anomaliaRepository.deleteAll();
        logRepository.deleteAll();

        when(streamingClient.openZipStream("data_carga_siconv.zip"))
                .thenAnswer(inv -> new ByteArrayInputStream(MockSiconvArchiveGenerator.generateSentinelaZip("23/09/2026 06:36:22")));

        when(streamingClient.openZipStream("siconv_proponentes.zip"))
                .thenAnswer(inv -> new ByteArrayInputStream(MockSiconvArchiveGenerator.generateProponentesZip()));

        when(streamingClient.openZipStream("siconv_proposta.zip"))
                .thenAnswer(inv -> new ByteArrayInputStream(MockSiconvArchiveGenerator.generatePropostasZip()));

        when(streamingClient.openZipStream("siconv_convenio.zip"))
                .thenAnswer(inv -> new ByteArrayInputStream(MockSiconvArchiveGenerator.generateConveniosZip()));
    }

    @Test
    @DisplayName("POST /api/v1/transferegov/sync/trigger deve acionar a sincronização com sucesso")
    void deveAcionarSincronizacao() throws Exception {
        mockMvc.perform(post("/api/v1/transferegov/sync/trigger?force=true&async=false")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ALERTA"))
                .andExpect(jsonPath("$.message").value("Execução do pipeline finalizada."));
    }

    @Test
    @DisplayName("GET /api/v1/transferegov/sync/status deve retornar a última execução")
    void deveRetornarStatusUltimaSincronizacao() throws Exception {
        SincronizacaoLogEntity log = new SincronizacaoLogEntity();
        log.setTipoSincronizacao("SICONV_CSV_DUMP");
        log.setStatus("SUCESSO");
        log.setDataInicio(OffsetDateTime.now());
        log.setTotalRegistrosPersistidos(45);
        logRepository.save(log);

        mockMvc.perform(get("/api/v1/transferegov/sync/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCESSO"))
                .andExpect(jsonPath("$.totalRegistrosPersistidos").value(45));
    }

    @Test
    @DisplayName("GET /api/v1/transferegov/sync/metrics deve consolidar métricas de qualidade")
    void deveRetornarMetricasDataQuality() throws Exception {
        SincronizacaoConvenioEntity conv = new SincronizacaoConvenioEntity();
        conv.setNrConvenio("912345");
        conv.setCnpjProponente("08923456000112");
        conv.setNomeProponente("Massaranduba");
        conv.setMunicipio("Massaranduba");
        conv.setUf("PB");
        conv.setValorGlobal(new BigDecimal("500000.00"));
        convenioRepository.save(conv);

        mockMvc.perform(get("/api/v1/transferegov/sync/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalConveniosCadastrados").value(1))
                .andExpect(jsonPath("$.totalConveniosUfPb").value(1))
                .andExpect(jsonPath("$.taxaQualidadeSucesso").value(100.0));
    }

    @Test
    @DisplayName("GET /api/v1/transferegov/convenios deve listar convênios paginados e detalhar por número")
    void deveConsultarConvenios() throws Exception {
        SincronizacaoConvenioEntity conv = new SincronizacaoConvenioEntity();
        conv.setNrConvenio("912345");
        conv.setCnpjProponente("08923456000112");
        conv.setNomeProponente("Prefeitura de Massaranduba");
        conv.setMunicipio("Massaranduba");
        conv.setUf("PB");
        conv.setValorGlobal(new BigDecimal("500000.00"));
        conv.setDataFimVigencia(LocalDate.of(2026, 12, 31));
        convenioRepository.save(conv);

        // Listagem
        mockMvc.perform(get("/api/v1/transferegov/convenios?uf=PB"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nrConvenio").value("912345"))
                .andExpect(jsonPath("$.content[0].municipio").value("Massaranduba"));

        // Detalhe por número
        mockMvc.perform(get("/api/v1/transferegov/convenios/912345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nrConvenio").value("912345"))
                .andExpect(jsonPath("$.valorGlobal").value(500000.00));
    }
}
