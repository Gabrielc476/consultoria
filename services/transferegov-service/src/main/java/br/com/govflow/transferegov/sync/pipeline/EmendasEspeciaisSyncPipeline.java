package br.com.govflow.transferegov.sync.pipeline;

import br.com.govflow.transferegov.config.EspeciaisProperties;
import br.com.govflow.transferegov.domain.compliance.Adpf854ComplianceEvaluator;
import br.com.govflow.transferegov.domain.compliance.Adpf854ComplianceResult;
import br.com.govflow.transferegov.domain.compliance.Adpf854ComplianceResult.InconformidadeItem;
import br.com.govflow.transferegov.domain.compliance.StatusAdpf854;
import br.com.govflow.transferegov.event.model.AlertaInconformidadeAdpf854Event;
import br.com.govflow.transferegov.event.producer.TransferegovEventPublisher;
import br.com.govflow.transferegov.persistence.entity.*;
import br.com.govflow.transferegov.persistence.repository.*;
import br.com.govflow.transferegov.sync.client.MonitoredCnpjProvider;
import br.com.govflow.transferegov.sync.client.TransferegovEspeciaisClient;
import br.com.govflow.transferegov.sync.client.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class EmendasEspeciaisSyncPipeline {

    private static final Logger log = LoggerFactory.getLogger(EmendasEspeciaisSyncPipeline.class);

    private final TransferegovEspeciaisClient especiaisClient;
    private final MonitoredCnpjProvider cnpjProvider;
    private final EspeciaisProperties properties;
    private final Adpf854ComplianceEvaluator complianceEvaluator;
    private final EmendaEspecialPlanoAcaoRepository planoAcaoRepository;
    private final EmendaEspecialPlanoTrabalhoRepository planoTrabalhoRepository;
    private final EmendaEspecialRelatorioGestaoRepository relatorioGestaoRepository;
    private final EmendaEspecialInconformidadeRepository inconformidadeRepository;
    private final SincronizacaoLogRepository logRepository;
    private final TransferegovEventPublisher eventPublisher;

    public EmendasEspeciaisSyncPipeline(
            TransferegovEspeciaisClient especiaisClient,
            MonitoredCnpjProvider cnpjProvider,
            EspeciaisProperties properties,
            Adpf854ComplianceEvaluator complianceEvaluator,
            EmendaEspecialPlanoAcaoRepository planoAcaoRepository,
            EmendaEspecialPlanoTrabalhoRepository planoTrabalhoRepository,
            EmendaEspecialRelatorioGestaoRepository relatorioGestaoRepository,
            EmendaEspecialInconformidadeRepository inconformidadeRepository,
            SincronizacaoLogRepository logRepository,
            TransferegovEventPublisher eventPublisher
    ) {
        this.especiaisClient = especiaisClient;
        this.cnpjProvider = cnpjProvider;
        this.properties = properties;
        this.complianceEvaluator = complianceEvaluator;
        this.planoAcaoRepository = planoAcaoRepository;
        this.planoTrabalhoRepository = planoTrabalhoRepository;
        this.relatorioGestaoRepository = relatorioGestaoRepository;
        this.inconformidadeRepository = inconformidadeRepository;
        this.logRepository = logRepository;
        this.eventPublisher = eventPublisher;
    }

    public SincronizacaoLogEntity executeSync(boolean force) {
        long startTime = System.currentTimeMillis();

        SincronizacaoLogEntity logEntity = new SincronizacaoLogEntity();
        logEntity.setTipoSincronizacao("EMENDAS_ESPECIAIS_REST");
        logEntity.setStatus("EM_ANDAMENTO");
        logEntity.setDataInicio(OffsetDateTime.now());
        logEntity = logRepository.save(logEntity);

        log.info("Iniciando pipeline de sincronização de Emendas Especiais REST [Log ID: {}]", logEntity.getId());

        long totalLidos = 0;
        long totalFiltrados = 0;
        long totalPersistidos = 0;
        long totalAnomalias = 0;

        try {
            String uf = properties.ufFilter();
            Set<String> monitoredCnpjs = new HashSet<>(cnpjProvider.getMonitoredCnpjs());
            if (properties.targetCnpjs() != null) {
                monitoredCnpjs.addAll(properties.targetCnpjs());
            }

            log.info("Buscando beneficiários especiais para UF='{}'...", uf);
            List<BeneficiarioEspecialDTO> beneficiarios = especiaisClient.consultarBeneficiarios(uf, null);
            log.info("Total de beneficiários retornados: {}", beneficiarios.size());

            Map<Long, BeneficiarioEspecialDTO> beneficiarioMap = new HashMap<>();
            for (BeneficiarioEspecialDTO b : beneficiarios) {
                if (b.idBeneficiario() == null) continue;

                if (!monitoredCnpjs.isEmpty()) {
                    String cnpjLimpo = b.cnpjBeneficiario() != null ? b.cnpjBeneficiario().replaceAll("\\D", "") : "";
                    boolean match = monitoredCnpjs.stream()
                            .map(c -> c.replaceAll("\\D", ""))
                            .anyMatch(c -> c.equals(cnpjLimpo));
                    if (match) {
                        beneficiarioMap.put(b.idBeneficiario(), b);
                    }
                } else {
                    beneficiarioMap.put(b.idBeneficiario(), b);
                }
            }

            totalFiltrados = beneficiarioMap.size();
            log.info("Beneficiários retidos após filtragem por monitoramento: {}", totalFiltrados);

            LocalDate hoje = LocalDate.now();

            for (BeneficiarioEspecialDTO benef : beneficiarioMap.values()) {
                List<PlanoAcaoEspecialDTO> planosAcao = especiaisClient.consultarPlanosAcao(benef.idBeneficiario(), null);
                totalLidos += planosAcao.size();

                for (PlanoAcaoEspecialDTO planoDto : planosAcao) {
                    List<PlanoTrabalhoEspecialDTO> pts = especiaisClient.consultarPlanosTrabalho(planoDto.idPlanoAcao());
                    List<RelatorioGestaoEspecialDTO> rgs = especiaisClient.consultarRelatoriosGestao(planoDto.idPlanoAcao());

                    Adpf854ComplianceResult complianceResult = complianceEvaluator.avaliar(planoDto, pts, rgs, hoje);

                    salvarEmendaCompleta(benef, planoDto, pts, rgs, complianceResult);
                    totalPersistidos++;

                    if (complianceResult.status() != StatusAdpf854.CONFORME && !complianceResult.inconformidades().isEmpty()) {
                        totalAnomalias += complianceResult.inconformidades().size();
                        notificarInconformidades(benef, planoDto, complianceResult);
                    }
                }
            }

            logEntity.setStatus(totalAnomalias > 0 ? "ALERTA" : "SUCESSO");
            logEntity.setTotalRegistrosLidos(totalLidos);
            logEntity.setTotalRegistrosFiltrados(totalFiltrados);
            logEntity.setTotalRegistrosPersistidos(totalPersistidos);
            logEntity.setTotalAnomalias(totalAnomalias);
            logEntity.setDataFim(OffsetDateTime.now());
            logEntity.setTempoExecucaoMs(System.currentTimeMillis() - startTime);

            log.info("Sincronização de Emendas Especiais concluída em {}ms! Lidos: {}, Persistidos: {}, Não-conformes: {}",
                    logEntity.getTempoExecucaoMs(), totalLidos, totalPersistidos, totalAnomalias);

            return logRepository.save(logEntity);

        } catch (Exception ex) {
            log.error("Erro durante pipeline de sincronização de Emendas Especiais: {}", ex.getMessage(), ex);
            logEntity.setStatus("FALHA");
            logEntity.setMensagemErro(ex.getMessage());
            logEntity.setDataFim(OffsetDateTime.now());
            logEntity.setTempoExecucaoMs(System.currentTimeMillis() - startTime);
            return logRepository.save(logEntity);
        }
    }

    @Transactional
    public void salvarEmendaCompleta(
            BeneficiarioEspecialDTO benef,
            PlanoAcaoEspecialDTO planoDto,
            List<PlanoTrabalhoEspecialDTO> pts,
            List<RelatorioGestaoEspecialDTO> rgs,
            Adpf854ComplianceResult complianceResult
    ) {
        EmendaEspecialPlanoAcaoEntity entityToSave = planoAcaoRepository.findByIdPlanoAcao(planoDto.idPlanoAcao())
                .map(existing -> {
                    existing.updateFromDto(planoDto, benef, complianceResult.status());
                    return existing;
                })
                .orElseGet(() -> EmendaEspecialPlanoAcaoEntity.fromDto(planoDto, benef, complianceResult.status()));

        final EmendaEspecialPlanoAcaoEntity savedEntity = planoAcaoRepository.save(entityToSave);

        // Sincronizar Planos de Trabalho
        if (pts != null) {
            for (PlanoTrabalhoEspecialDTO ptDto : pts) {
                if (ptDto.idPlanoTrabalho() == null) continue;
                EmendaEspecialPlanoTrabalhoEntity ptEntity = planoTrabalhoRepository.findByIdPlanoTrabalho(ptDto.idPlanoTrabalho())
                        .map(existing -> {
                            existing.updateFromDto(ptDto);
                            return existing;
                        })
                        .orElseGet(() -> EmendaEspecialPlanoTrabalhoEntity.fromDto(savedEntity, ptDto));

                planoTrabalhoRepository.save(ptEntity);
            }
        }

        // Sincronizar Relatórios de Gestão
        if (rgs != null) {
            for (RelatorioGestaoEspecialDTO rgDto : rgs) {
                if (rgDto.idRelatorioGestaoNovo() == null) continue;
                EmendaEspecialRelatorioGestaoEntity rgEntity = relatorioGestaoRepository.findByIdRelatorioGestaoNovo(rgDto.idRelatorioGestaoNovo())
                        .map(existing -> {
                            existing.updateFromDto(rgDto);
                            return existing;
                        })
                        .orElseGet(() -> EmendaEspecialRelatorioGestaoEntity.fromDto(savedEntity, rgDto));

                relatorioGestaoRepository.save(rgEntity);
            }
        }

        // Sincronizar Inconformidades da ADPF 854
        List<EmendaEspecialInconformidadeEntity> anteriores = inconformidadeRepository.findByPlanoAcaoIdAndResolvidoFalse(savedEntity.getId());
        for (EmendaEspecialInconformidadeEntity ant : anteriores) {
            ant.setResolvido(true);
            ant.setDataResolucao(OffsetDateTime.now());
            inconformidadeRepository.save(ant);
        }

        for (InconformidadeItem item : complianceResult.inconformidades()) {
            EmendaEspecialInconformidadeEntity inconf = EmendaEspecialInconformidadeEntity.fromCompliance(savedEntity, item);
            inconformidadeRepository.save(inconf);
        }
    }

    private void notificarInconformidades(
            BeneficiarioEspecialDTO benef,
            PlanoAcaoEspecialDTO plano,
            Adpf854ComplianceResult complianceResult
    ) {
        for (InconformidadeItem item : complianceResult.inconformidades()) {
            AlertaInconformidadeAdpf854Event event = AlertaInconformidadeAdpf854Event.of(
                    plano.idPlanoAcao(),
                    plano.codigoPlanoAcao(),
                    benef.nomeBeneficiario(),
                    benef.ufBeneficiario(),
                    benef.cnpjBeneficiario(),
                    plano.nomeParlamentarEmendaPlanoAcao(),
                    plano.anoEmendaParlamentarPlanoAcao(),
                    plano.valorTotal(),
                    complianceResult.status(),
                    item.tipo().name(),
                    item.severidade().name(),
                    item.descricao()
            );
            eventPublisher.publicarAlertaInconformidadeAdpf854(event);
        }
    }
}
