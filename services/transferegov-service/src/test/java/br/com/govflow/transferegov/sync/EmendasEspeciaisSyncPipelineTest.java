package br.com.govflow.transferegov.sync;

import br.com.govflow.transferegov.domain.compliance.StatusAdpf854;
import br.com.govflow.transferegov.event.model.AlertaInconformidadeAdpf854Event;
import br.com.govflow.transferegov.event.producer.TransferegovEventPublisher;
import br.com.govflow.transferegov.persistence.entity.EmendaEspecialPlanoAcaoEntity;
import br.com.govflow.transferegov.persistence.entity.SincronizacaoLogEntity;
import br.com.govflow.transferegov.persistence.repository.*;
import br.com.govflow.transferegov.sync.client.TransferegovEspeciaisClient;
import br.com.govflow.transferegov.sync.client.dto.*;
import br.com.govflow.transferegov.sync.pipeline.EmendasEspeciaisSyncPipeline;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Testes de Integração do Pipeline de Ingestão de Emendas Especiais (Pix) e Auditoria ADPF 854")
class EmendasEspeciaisSyncPipelineTest {

    @Autowired
    private EmendasEspeciaisSyncPipeline pipeline;

    @Autowired
    private EmendaEspecialPlanoAcaoRepository planoAcaoRepository;

    @Autowired
    private EmendaEspecialPlanoTrabalhoRepository planoTrabalhoRepository;

    @Autowired
    private EmendaEspecialRelatorioGestaoRepository relatorioGestaoRepository;

    @Autowired
    private EmendaEspecialInconformidadeRepository inconformidadeRepository;

    @Autowired
    private SincronizacaoLogRepository logRepository;

    @MockBean
    private TransferegovEspeciaisClient especiaisClient;

    @MockBean
    private TransferegovEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        inconformidadeRepository.deleteAll();
        relatorioGestaoRepository.deleteAll();
        planoTrabalhoRepository.deleteAll();
        planoAcaoRepository.deleteAll();
        logRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve executar sincronização completa persistindo entidades e disparando alertas para inconformidades críticas")
    void deveExecutarSincronizacaoCompletaComSucesso() {
        BeneficiarioEspecialDTO benef = new BeneficiarioEspecialDTO(
                101L, "PB", "PREFEITURA DE SOUSA", "08847784000144", 1L
        );
        when(especiaisClient.consultarBeneficiarios(eq("PB"), any()))
                .thenReturn(List.of(benef));

        // Plano 1: Conforme (possui plano de trabalho aprovado e relatório disponibilizado)
        PlanoAcaoEspecialDTO plano1 = new PlanoAcaoEspecialDTO(
                7001L, "2024.7001", 2024, "TRANSFERENCIA_ESPECIAL", "CIENTE",
                "contato@sousa.pb.gov.br", LocalDate.of(2024, 1, 15),
                "001", 1, "REGULAR", "BANCO DO BRASIL", "1234", "5", "98765", "1",
                "DEPUTADO A", 2024, 111, 1, 20240001, "2024.0001",
                "INVESTIMENTO", "Infraestrutura", "Asfalto", null,
                BigDecimal.ZERO, new BigDecimal("400000.00"), "1234-98765",
                101L, 501, "Pavimentação", "Obras urbanas", 1
        );

        PlanoTrabalhoEspecialDTO pt1 = new PlanoTrabalhoEspecialDTO(
                9001L, "2024-02-01 10:00:00", "APROVADO", "Não",
                LocalDate.of(2024, 2, 1), LocalDate.of(2025, 2, 1), 12,
                7001L, "44.90.51", "NÃO", "NÃO", null, "2024-02-10 10:00:00", "Não"
        );

        RelatorioGestaoEspecialDTO rg1 = new RelatorioGestaoEspecialDTO(
                8001L, LocalDate.of(2025, 3, 1), "2025-03-01 10:00:00", "Final",
                new BigDecimal("400000.00"), BigDecimal.ZERO, "DISPONIBILIZADO", 7001L, 10
        );

        // Plano 2: Não Conforme (sem plano de trabalho e sem relatório)
        PlanoAcaoEspecialDTO plano2 = new PlanoAcaoEspecialDTO(
                7002L, "2024.7002", 2024, "TRANSFERENCIA_ESPECIAL", "CIENTE",
                "contato@sousa.pb.gov.br", LocalDate.of(2024, 3, 20),
                "001", 1, "REGULAR", "BANCO DO BRASIL", "1234", "5", "98766", "2",
                "DEPUTADO B", 2024, 222, 2, 20240002, "2024.0002",
                "CUSTEIO", "Saúde", "Insumos", null,
                new BigDecimal("250000.00"), BigDecimal.ZERO, "1234-98766",
                101L, 502, "Medicamentos", "Atenção primária", 1
        );

        when(especiaisClient.consultarPlanosAcao(eq(101L), any()))
                .thenReturn(List.of(plano1, plano2));

        when(especiaisClient.consultarPlanosTrabalho(eq(7001L)))
                .thenReturn(List.of(pt1));
        when(especiaisClient.consultarRelatoriosGestao(eq(7001L)))
                .thenReturn(List.of(rg1));

        when(especiaisClient.consultarPlanosTrabalho(eq(7002L)))
                .thenReturn(Collections.emptyList());
        when(especiaisClient.consultarRelatoriosGestao(eq(7002L)))
                .thenReturn(Collections.emptyList());

        // Executar
        SincronizacaoLogEntity logResult = pipeline.executeSync(false);

        // Validações de Log
        assertThat(logResult).isNotNull();
        assertThat(logResult.getStatus()).isEqualTo("ALERTA");
        assertThat(logResult.getTotalRegistrosLidos()).isEqualTo(2);
        assertThat(logResult.getTotalRegistrosPersistidos()).isEqualTo(2);
        assertThat(logResult.getTotalAnomalias()).isEqualTo(1);

        // Validações de Banco de Dados
        assertThat(planoAcaoRepository.count()).isEqualTo(2);
        EmendaEspecialPlanoAcaoEntity e1 = planoAcaoRepository.findByIdPlanoAcao(7001L).orElseThrow();
        assertThat(e1.getStatusAdpf854()).isEqualTo(StatusAdpf854.CONFORME);
        assertThat(e1.getValorTotal()).isEqualByComparingTo(new BigDecimal("400000.00"));

        EmendaEspecialPlanoAcaoEntity e2 = planoAcaoRepository.findByIdPlanoAcao(7002L).orElseThrow();
        assertThat(e2.getStatusAdpf854()).isEqualTo(StatusAdpf854.NAO_CONFORME);
        assertThat(e2.getValorTotal()).isEqualByComparingTo(new BigDecimal("250000.00"));

        assertThat(planoTrabalhoRepository.count()).isEqualTo(1);
        assertThat(relatorioGestaoRepository.count()).isEqualTo(1);
        assertThat(inconformidadeRepository.count()).isGreaterThanOrEqualTo(1);

        // Validações de Evento RabbitMQ
        ArgumentCaptor<AlertaInconformidadeAdpf854Event> captor = ArgumentCaptor.forClass(AlertaInconformidadeAdpf854Event.class);
        verify(eventPublisher, times(1)).publicarAlertaInconformidadeAdpf854(captor.capture());

        AlertaInconformidadeAdpf854Event evento = captor.getValue();
        assertThat(evento.idPlanoAcao()).isEqualTo(7002L);
        assertThat(evento.codigoPlanoAcao()).isEqualTo("2024.7002");
        assertThat(evento.municipio()).isEqualTo("PREFEITURA DE SOUSA");
        assertThat(evento.statusAdpf854()).isEqualTo(StatusAdpf854.NAO_CONFORME);
    }

    @Test
    @DisplayName("Deve garantir idempotência ao reexecutar a sincronização")
    void deveGarantirIdempotencia() {
        BeneficiarioEspecialDTO benef = new BeneficiarioEspecialDTO(
                102L, "PB", "PREFEITURA DE PATOS", "09123456000188", 2L
        );
        when(especiaisClient.consultarBeneficiarios(eq("PB"), any()))
                .thenReturn(List.of(benef));

        PlanoAcaoEspecialDTO plano = new PlanoAcaoEspecialDTO(
                7003L, "2024.7003", 2024, "TRANSFERENCIA_ESPECIAL", "CIENTE",
                "contato@patos.pb.gov.br", LocalDate.of(2024, 2, 10),
                "001", 1, "REGULAR", "BANCO DO BRASIL", "4321", "9", "12345", "6",
                "DEPUTADO C", 2024, 333, 1, 20240003, "2024.0003",
                "INVESTIMENTO", "Esporte", "Quadra Poliesportiva", null,
                BigDecimal.ZERO, new BigDecimal("300000.00"), "4321-12345",
                102L, 503, "Construção de Quadra", "Bairro Novo", 2
        );

        when(especiaisClient.consultarPlanosAcao(eq(102L), any()))
                .thenReturn(List.of(plano));
        when(especiaisClient.consultarPlanosTrabalho(eq(7003L)))
                .thenReturn(Collections.emptyList());
        when(especiaisClient.consultarRelatoriosGestao(eq(7003L)))
                .thenReturn(Collections.emptyList());

        // 1ª execução
        pipeline.executeSync(false);
        assertThat(planoAcaoRepository.count()).isEqualTo(1);

        // 2ª execução (mesmo plano)
        pipeline.executeSync(true);
        assertThat(planoAcaoRepository.count()).isEqualTo(1);
    }
}
