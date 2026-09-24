package br.com.govflow.transferegov.sync;

import br.com.govflow.transferegov.persistence.entity.SincronizacaoAnomaliaEntity;
import br.com.govflow.transferegov.persistence.entity.SincronizacaoConvenioEntity;
import br.com.govflow.transferegov.persistence.entity.SincronizacaoLogEntity;
import br.com.govflow.transferegov.persistence.repository.SincronizacaoAnomaliaRepository;
import br.com.govflow.transferegov.persistence.repository.SincronizacaoConvenioRepository;
import br.com.govflow.transferegov.persistence.repository.SincronizacaoLogRepository;
import br.com.govflow.transferegov.sync.client.SiconvStreamingClient;
import br.com.govflow.transferegov.sync.mock.MockSiconvArchiveGenerator;
import br.com.govflow.transferegov.sync.pipeline.SiconvStreamingPipeline;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Testes de Integração do Pipeline de Streaming SICONV (Zero Disk I/O & Data Quality)")
class SiconvStreamingPipelineTest {

    @Autowired
    private SiconvStreamingPipeline pipeline;

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

        // Configura streams em memória sem gravar no disco
        when(streamingClient.openZipStream(eq("data_carga_siconv.zip")))
                .thenAnswer(inv -> new ByteArrayInputStream(MockSiconvArchiveGenerator.generateSentinelaZip("23/09/2026 06:36:22")));

        when(streamingClient.openZipStream(eq("siconv_proponentes.zip")))
                .thenAnswer(inv -> new ByteArrayInputStream(MockSiconvArchiveGenerator.generateProponentesZip()));

        when(streamingClient.openZipStream(eq("siconv_proposta.zip")))
                .thenAnswer(inv -> new ByteArrayInputStream(MockSiconvArchiveGenerator.generatePropostasZip()));

        when(streamingClient.openZipStream(eq("siconv_convenio.zip")))
                .thenAnswer(inv -> new ByteArrayInputStream(MockSiconvArchiveGenerator.generateConveniosZip()));
    }

    @Test
    @DisplayName("Deve executar o pipeline completo em streaming, persistir convênios da PB e isolar anomalias na quarentena")
    void deveExecutarPipelineCompletoComSucessoEQuarentena() {
        SincronizacaoLogEntity log = pipeline.executeSync(false);

        assertNotNull(log);
        assertEquals("ALERTA", log.getStatus(), "Status deve ser ALERTA pois há 1 registro inconsistente gerado no mock");
        assertEquals("23/09/2026 06:36:22", log.getDataCargaSiconvReferencia());
        assertEquals(2, log.getTotalRegistrosPersistidos(), "Deve ter persistido os 2 convênios válidos da Paraíba");
        assertEquals(1, log.getTotalAnomalias(), "Deve ter isolado 1 convênio anômalo");

        // Verifica que os convênios válidos estão no banco
        Optional<SincronizacaoConvenioEntity> c1 = convenioRepository.findByNrConvenio("912345");
        assertTrue(c1.isPresent());
        assertEquals("Massaranduba", c1.get().getMunicipio());
        assertEquals("PB", c1.get().getUf());
        assertEquals(new BigDecimal("500000.00"), c1.get().getValorGlobal());

        Optional<SincronizacaoConvenioEntity> c2 = convenioRepository.findByNrConvenio("912346");
        assertTrue(c2.isPresent());
        assertEquals("Joao Pessoa", c2.get().getMunicipio());

        // Verifica que o convênio fora da PB não foi persistido
        Optional<SincronizacaoConvenioEntity> cSp = convenioRepository.findByNrConvenio("999999");
        assertFalse(cSp.isPresent(), "Convênio de SP não deve ser persistido");

        // Verifica o isolamento da anomalia na quarentena
        List<SincronizacaoAnomaliaEntity> anomalias = anomaliaRepository.findByLogId(log.getId());
        assertEquals(1, anomalias.size());
        assertEquals("912347", anomalias.get(0).getIdentificadorRegistro());
        assertEquals("CONSISTENCY", anomalias.get(0).getDimensaoQualidade());
    }

    @Test
    @DisplayName("Deve dispensar nova carga quando sentinela de data for idêntica e force for falso")
    void deveDispensarCargaRepetida() {
        // Primeira execução
        SincronizacaoLogEntity log1 = pipeline.executeSync(false);
        assertEquals("ALERTA", log1.getStatus());

        // Altera status do primeiro log para SUCESSO para simular última carga concluída com perfeição
        log1.setStatus("SUCESSO");
        logRepository.save(log1);

        // Segunda execução com force = false
        SincronizacaoLogEntity log2 = pipeline.executeSync(false);

        assertEquals("SUCESSO_SEM_ALTERACOES", log2.getStatus());
        assertEquals(0, log2.getTotalRegistrosLidos());
    }

    @Test
    @DisplayName("Deve reprocessar e atualizar idempotentemente os registros quando force for verdadeiro")
    void deveReprocessarComForceVerdadeiro() {
        // Primeira execução
        pipeline.executeSync(false);
        assertEquals(2, convenioRepository.count());

        // Segunda execução forçada
        SincronizacaoLogEntity logForcado = pipeline.executeSync(true);

        assertEquals("ALERTA", logForcado.getStatus());
        assertEquals(2, convenioRepository.count(), "Não deve duplicar registros no banco (UPSERT idempotente)");
    }
}
