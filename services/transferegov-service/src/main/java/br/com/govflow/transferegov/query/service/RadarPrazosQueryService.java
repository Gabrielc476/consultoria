package br.com.govflow.transferegov.query.service;

import br.com.govflow.transferegov.domain.radar.AlertaConvenioDTO;
import br.com.govflow.transferegov.domain.radar.DeadlineDetectorService;
import br.com.govflow.transferegov.domain.radar.NivelRisco;
import br.com.govflow.transferegov.event.producer.TransferegovEventPublisher;
import br.com.govflow.transferegov.persistence.entity.SincronizacaoConvenioEntity;
import br.com.govflow.transferegov.persistence.repository.SincronizacaoConvenioRepository;
import br.com.govflow.transferegov.query.dto.MunicipioRadarDTO;
import br.com.govflow.transferegov.query.dto.RadarPrazosResponseDTO;
import br.com.govflow.transferegov.query.dto.ResumoRadarDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Serviço de aplicação para consolidação, agrupamento e filtragem de convênios no Radar de Prazos.
 */
@Service
public class RadarPrazosQueryService {

    private static final Logger log = LoggerFactory.getLogger(RadarPrazosQueryService.class);

    private final SincronizacaoConvenioRepository convenioRepository;
    private final DeadlineDetectorService deadlineDetectorService;
    private final TransferegovEventPublisher eventPublisher;

    public RadarPrazosQueryService(
            SincronizacaoConvenioRepository convenioRepository,
            DeadlineDetectorService deadlineDetectorService,
            TransferegovEventPublisher eventPublisher
    ) {
        this.convenioRepository = convenioRepository;
        this.deadlineDetectorService = deadlineDetectorService;
        this.eventPublisher = eventPublisher;
    }

    public RadarPrazosResponseDTO obterRadar(
            String uf,
            String cnpj,
            String municipio,
            NivelRisco nivelRisco,
            LocalDate dataReferencia
    ) {
        LocalDate ref = dataReferencia != null ? dataReferencia : LocalDate.now();

        List<SincronizacaoConvenioEntity> entidades = convenioRepository.findAtivosPorFiltros(uf, cnpj, municipio);

        List<AlertaConvenioDTO> todosAlertas = entidades.stream()
                .map(e -> deadlineDetectorService.avaliar(e, ref))
                .filter(Objects::nonNull)
                .toList();

        // 1. Resumo Geral de KPIs
        long totalMonitorados = todosAlertas.size();
        long totalCriticos = todosAlertas.stream().filter(a -> a.nivelRisco() == NivelRisco.CRITICO).count();
        long totalAtencao = todosAlertas.stream().filter(a -> a.nivelRisco() == NivelRisco.ATENCAO).count();
        long totalRegulares = todosAlertas.stream().filter(a -> a.nivelRisco() == NivelRisco.REGULAR).count();

        ResumoRadarDTO resumo = new ResumoRadarDTO(
                totalMonitorados,
                totalCriticos,
                totalAtencao,
                totalRegulares,
                ref
        );

        // 2. Agrupamento Consolidado por Município
        Map<String, List<AlertaConvenioDTO>> porMunicipio = todosAlertas.stream()
                .collect(Collectors.groupingBy(a -> a.municipio() + "::" + a.uf() + "::" + a.cnpjProponente()));

        List<MunicipioRadarDTO> municipios = porMunicipio.entrySet().stream()
                .map(entry -> {
                    List<AlertaConvenioDTO> alertasMun = entry.getValue();
                    AlertaConvenioDTO primeiro = alertasMun.getFirst();

                    long countTotal = alertasMun.size();
                    long countCrit = alertasMun.stream().filter(a -> a.nivelRisco() == NivelRisco.CRITICO).count();
                    long countAtencao = alertasMun.stream().filter(a -> a.nivelRisco() == NivelRisco.ATENCAO).count();
                    long countRegular = alertasMun.stream().filter(a -> a.nivelRisco() == NivelRisco.REGULAR).count();

                    NivelRisco maiorRisco = NivelRisco.REGULAR;
                    if (countCrit > 0) {
                        maiorRisco = NivelRisco.CRITICO;
                    } else if (countAtencao > 0) {
                        maiorRisco = NivelRisco.ATENCAO;
                    }

                    return new MunicipioRadarDTO(
                            primeiro.municipio(),
                            primeiro.uf(),
                            primeiro.cnpjProponente(),
                            primeiro.nomeProponente(),
                            countTotal,
                            countCrit,
                            countAtencao,
                            countRegular,
                            maiorRisco
                    );
                })
                .sorted(Comparator.comparingInt((MunicipioRadarDTO m) -> m.maiorRisco().getPrioridade())
                        .thenComparing(MunicipioRadarDTO::municipio))
                .toList();

        // 3. Filtragem e ordenação dos alertas detalhados
        List<AlertaConvenioDTO> alertasFiltrados = todosAlertas.stream()
                .filter(a -> nivelRisco == null || a.nivelRisco() == nivelRisco)
                .sorted(Comparator.comparing(
                        AlertaConvenioDTO::diasRestantes,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .toList();

        return new RadarPrazosResponseDTO(resumo, municipios, alertasFiltrados);
    }

    /**
     * Avalia todos os convênios ativos no banco e dispara eventos no RabbitMQ para convênios críticos.
     *
     * @param dataReferencia Data base de cálculo (ou LocalDate.now() se null)
     * @return Total de alertas críticos emitidos
     */
    public int avaliarPrazosEAlertar(LocalDate dataReferencia) {
        LocalDate ref = dataReferencia != null ? dataReferencia : LocalDate.now();
        List<SincronizacaoConvenioEntity> ativos = convenioRepository.findByInstrumentoAtivoTrue();

        int emitidos = 0;
        for (SincronizacaoConvenioEntity convenio : ativos) {
            AlertaConvenioDTO alerta = deadlineDetectorService.avaliar(convenio, ref);
            if (alerta != null && alerta.nivelRisco() == NivelRisco.CRITICO) {
                eventPublisher.publicarAlertaPrazoSeCritico(alerta);
                emitidos++;
            }
        }

        log.info("Avaliação de prazos concluída para {} convênios. Alertas críticos emitidos para o RabbitMQ: {}",
                ativos.size(), emitidos);
        return emitidos;
    }
}
