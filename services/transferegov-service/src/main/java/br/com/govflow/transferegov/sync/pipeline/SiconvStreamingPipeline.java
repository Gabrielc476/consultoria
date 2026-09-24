package br.com.govflow.transferegov.sync.pipeline;

import br.com.govflow.transferegov.config.SiconvProperties;
import br.com.govflow.transferegov.domain.model.ConvenioSincronizado;
import br.com.govflow.transferegov.domain.model.ProponenteInfo;
import br.com.govflow.transferegov.domain.model.PropostaInfo;
import br.com.govflow.transferegov.domain.quality.DataQualityIssue;
import br.com.govflow.transferegov.domain.quality.DataQualityReport;
import br.com.govflow.transferegov.domain.quality.DataQualityResult;
import br.com.govflow.transferegov.domain.quality.SiconvDataQualityValidator;
import br.com.govflow.transferegov.persistence.entity.SincronizacaoAnomaliaEntity;
import br.com.govflow.transferegov.persistence.entity.SincronizacaoLogEntity;
import br.com.govflow.transferegov.persistence.repository.SincronizacaoAnomaliaRepository;
import br.com.govflow.transferegov.persistence.repository.SincronizacaoLogRepository;
import br.com.govflow.transferegov.sync.client.MonitoredCnpjProvider;
import br.com.govflow.transferegov.sync.client.SiconvStreamingClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class SiconvStreamingPipeline {

    private static final Logger log = LoggerFactory.getLogger(SiconvStreamingPipeline.class);

    private final SiconvStreamingClient streamingClient;
    private final SiconvCsvParser csvParser;
    private final SiconvDataQualityValidator qualityValidator;
    private final SiconvConvenioBatchWriter batchWriter;
    private final SincronizacaoLogRepository logRepository;
    private final SincronizacaoAnomaliaRepository anomaliaRepository;
    private final MonitoredCnpjProvider monitoredCnpjProvider;
    private final SiconvProperties properties;
    private final ObjectMapper objectMapper;

    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    public SiconvStreamingPipeline(
            SiconvStreamingClient streamingClient,
            SiconvCsvParser csvParser,
            SiconvDataQualityValidator qualityValidator,
            SiconvConvenioBatchWriter batchWriter,
            SincronizacaoLogRepository logRepository,
            SincronizacaoAnomaliaRepository anomaliaRepository,
            MonitoredCnpjProvider monitoredCnpjProvider,
            SiconvProperties properties,
            ObjectMapper objectMapper
    ) {
        this.streamingClient = streamingClient;
        this.csvParser = csvParser;
        this.qualityValidator = qualityValidator;
        this.batchWriter = batchWriter;
        this.logRepository = logRepository;
        this.anomaliaRepository = anomaliaRepository;
        this.monitoredCnpjProvider = monitoredCnpjProvider;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public boolean isRunning() {
        return isRunning.get();
    }

    /**
     * Executa a sincronização completa via streaming em memória dos dumps do SICONV.
     *
     * @param force Se true, força o processamento mesmo que a sentinela não tenha mudado
     * @return O registro de log da sincronização com métricas de Data Quality
     */
    public SincronizacaoLogEntity executeSync(boolean force) {
        if (!isRunning.compareAndSet(false, true)) {
            log.warn("Sincronização do SICONV já está em execução no momento. Ignorando nova solicitação.");
            return logRepository.findTopByOrderByDataInicioDesc().orElse(null);
        }

        SincronizacaoLogEntity logEntity = new SincronizacaoLogEntity();
        logEntity.setTipoSincronizacao("SICONV_CSV_DUMP");
        logEntity.setStatus("EM_ANDAMENTO");
        logEntity.setDataInicio(OffsetDateTime.now());
        logEntity = logRepository.save(logEntity);

        long startTime = System.currentTimeMillis();
        DataQualityReport report = new DataQualityReport();

        try {
            log.info("Iniciando pipeline de streaming SICONV [Log ID: {}] (force={})", logEntity.getId(), force);

            // 1. Sentinela de Carga
            String sentinela = null;
            try (InputStream is = streamingClient.openZipStream("data_carga_siconv.zip")) {
                sentinela = csvParser.readSentinelaDataCarga(is);
            } catch (Exception e) {
                log.warn("Não foi possível ler data_carga_siconv.zip ({}), prosseguindo...", e.getMessage());
            }

            log.info("Sentinela de carga diária SICONV identificada: {}", sentinela);
            logEntity.setDataCargaSiconvReferencia(sentinela);
            report.setDataCargaSiconv(sentinela);

            // Verificação de frescor/idempotência
            if (!force && sentinela != null) {
                Optional<SincronizacaoLogEntity> lastSuccess = logRepository.findTopByStatusOrderByDataInicioDesc("SUCESSO");
                if (lastSuccess.isPresent() && sentinela.equals(lastSuccess.get().getDataCargaSiconvReferencia())) {
                    log.info("Dump SICONV com carga '{}' já processado anteriormente. Carga dispensada.", sentinela);
                    logEntity.setStatus("SUCESSO_SEM_ALTERACOES");
                    logEntity.setDataFim(OffsetDateTime.now());
                    logEntity.setTempoExecucaoMs(System.currentTimeMillis() - startTime);
                    return logRepository.save(logEntity);
                }
            }

            // 2. Streaming de Proponentes (Filtro em voo pela UF e CNPJs monitorados)
            Map<String, ProponenteInfo> proponentes;
            try (InputStream is = streamingClient.openZipStream("siconv_proponentes.zip")) {
                proponentes = csvParser.streamProponentes(is, properties.ufFilter(), monitoredCnpjProvider.getMonitoredCnpjs());
            }

            // 3. Streaming de Propostas (Filtro em voo pelos Proponentes da UF)
            Map<String, PropostaInfo> propostas;
            try (InputStream is = streamingClient.openZipStream("siconv_proposta.zip")) {
                propostas = csvParser.streamPropostas(is, proponentes.keySet());
            }

            // 4. Streaming de Convênios com Data Quality e UPSERT em Batches
            List<ConvenioSincronizado> batchBuffer = new ArrayList<>(properties.batchSize());
            List<SincronizacaoAnomaliaEntity> anomaliaBuffer = new ArrayList<>();
            final UUID currentLogId = logEntity.getId();
            final String dataCargaFinal = sentinela;

            try (InputStream is = streamingClient.openZipStream("siconv_convenio.zip")) {
                csvParser.streamConvenios(is, proponentes, propostas, dataCargaFinal, (convenio, rowNumber) -> {
                    report.incrementRead();
                    report.incrementFiltered();

                    DataQualityResult result = qualityValidator.validate(convenio, rowNumber);

                    if (!result.valid()) {
                        for (DataQualityIssue issue : result.issues()) {
                            report.recordIssue(issue);
                            SincronizacaoAnomaliaEntity anomalia = new SincronizacaoAnomaliaEntity();
                            anomalia.setLogId(currentLogId);
                            anomalia.setOrigemArquivo("siconv_convenio.csv");
                            anomalia.setNumeroLinha(rowNumber);
                            anomalia.setIdentificadorRegistro(convenio.nrConvenio());
                            anomalia.setDimensaoQualidade(issue.dimension().name());
                            anomalia.setDescricaoFalha(issue.description());
                            anomalia.setConteudoBruto(issue.field() + "=" + issue.observedValue());
                            anomaliaBuffer.add(anomalia);
                        }
                    } else {
                        report.incrementValid();
                        batchBuffer.add(convenio);
                    }

                    if (batchBuffer.size() >= properties.batchSize()) {
                        batchWriter.writeBatch(batchBuffer);
                        batchBuffer.clear();
                    }

                    if (anomaliaBuffer.size() >= properties.batchSize()) {
                        anomaliaRepository.saveAll(anomaliaBuffer);
                        anomaliaBuffer.clear();
                    }
                });
            }

            // Flush final dos buffers remanescentes
            if (!batchBuffer.isEmpty()) {
                batchWriter.writeBatch(batchBuffer);
                batchBuffer.clear();
            }
            if (!anomaliaBuffer.isEmpty()) {
                anomaliaRepository.saveAll(anomaliaBuffer);
                anomaliaBuffer.clear();
            }

            // 5. Finalização e consolidação das métricas
            long duration = System.currentTimeMillis() - startTime;
            report.setExecutionTimeMs(duration);

            logEntity.setTotalRegistrosLidos(report.getTotalRead());
            logEntity.setTotalRegistrosFiltrados(report.getTotalFiltered());
            logEntity.setTotalRegistrosPersistidos(report.getTotalValid());
            logEntity.setTotalAnomalias(report.getTotalAnomalies());
            logEntity.setStatus(report.getTotalAnomalies() > 0 ? "ALERTA" : "SUCESSO");
            logEntity.setTempoExecucaoMs(duration);
            logEntity.setDataFim(OffsetDateTime.now());
            logEntity.setDetalhesExecucaoJson(objectMapper.writeValueAsString(report));

            log.info("Sincronização concluída em {}ms! Lidos: {}, Filtrados: {}, Persistidos: {}, Anomalias: {}",
                    duration, report.getTotalRead(), report.getTotalFiltered(), report.getTotalValid(), report.getTotalAnomalies());

            return logRepository.save(logEntity);

        } catch (Exception e) {
            log.error("Erro fatal durante o pipeline de streaming do SICONV: {}", e.getMessage(), e);
            logEntity.setStatus("FALHA");
            logEntity.setMensagemErro(e.getMessage());
            logEntity.setDataFim(OffsetDateTime.now());
            logEntity.setTempoExecucaoMs(System.currentTimeMillis() - startTime);
            return logRepository.save(logEntity);
        } finally {
            isRunning.set(false);
        }
    }
}
