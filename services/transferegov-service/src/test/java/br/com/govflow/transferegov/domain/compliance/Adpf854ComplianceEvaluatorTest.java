package br.com.govflow.transferegov.domain.compliance;

import br.com.govflow.transferegov.sync.client.dto.PlanoAcaoEspecialDTO;
import br.com.govflow.transferegov.sync.client.dto.PlanoTrabalhoEspecialDTO;
import br.com.govflow.transferegov.sync.client.dto.RelatorioGestaoEspecialDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Testes Unitários do Motor de Conformidade STF ADPF 854 (Adpf854ComplianceEvaluator)")
class Adpf854ComplianceEvaluatorTest {

    private Adpf854ComplianceEvaluator evaluator;
    private final LocalDate hoje = LocalDate.of(2026, 9, 25);

    @BeforeEach
    void setUp() {
        evaluator = new Adpf854ComplianceEvaluator();
    }

    private PlanoAcaoEspecialDTO criarPlanoAcaoPadrao(String situacao) {
        return new PlanoAcaoEspecialDTO(
                1001L,
                "2024.12345",
                2024,
                "TRANSFERENCIA_ESPECIAL",
                situacao,
                "camara@pombal.pb.gov.br",
                LocalDate.of(2024, 2, 10),
                "001",
                1,
                "CONTA_REGULAR",
                "BANCO DO BRASIL",
                "1234",
                "5",
                "98765",
                "2",
                "DEPUTADO FEDERAL EXECUTOR",
                2024,
                555,
                1,
                20240001,
                "2024.0001",
                "INVESTIMENTO",
                "Infraestrutura Urbana",
                "Pavimentação de Acessos",
                null,
                BigDecimal.ZERO,
                new BigDecimal("500000.00"),
                "1234-98765",
                2001L,
                301,
                "Pavimentação Asfáltica",
                "Obras nas ruas centrais",
                10
        );
    }

    @Test
    @DisplayName("Cenário 1: Deve classificar como CONFORME quando plano aprovado e relatório disponibilizado")
    void deveClassificarComoConforme() {
        PlanoAcaoEspecialDTO planoAcao = criarPlanoAcaoPadrao("CIENTE");

        PlanoTrabalhoEspecialDTO pt = new PlanoTrabalhoEspecialDTO(
                501L,
                "2024-03-01 10:00:00",
                "APROVADO",
                "Não",
                LocalDate.of(2024, 3, 1),
                LocalDate.of(2025, 3, 1),
                12,
                1001L,
                "44.90.51",
                "NÃO",
                "NÃO",
                null,
                "2024-03-15 14:00:00",
                "Não"
        );

        RelatorioGestaoEspecialDTO rg = new RelatorioGestaoEspecialDTO(
                801L,
                LocalDate.of(2025, 4, 1),
                "2025-04-01 16:00:00",
                "Final",
                new BigDecimal("500000.00"),
                BigDecimal.ZERO,
                "DISPONIBILIZADO",
                1001L,
                101
        );

        Adpf854ComplianceResult result = evaluator.avaliar(
                planoAcao,
                List.of(pt),
                List.of(rg),
                hoje
        );

        assertThat(result.status()).isEqualTo(StatusAdpf854.CONFORME);
        assertThat(result.possuiPlanoTrabalho()).isTrue();
        assertThat(result.planoTrabalhoAprovado()).isTrue();
        assertThat(result.possuiRelatorioGestao()).isTrue();
        assertThat(result.relatorioGestaoDisponibilizado()).isTrue();
        assertThat(result.dadosBancariosValidos()).isTrue();
        assertThat(result.inconformidades()).isEmpty();
    }

    @Test
    @DisplayName("Cenário 2: Deve classificar como NAO_CONFORME por ausência de plano de trabalho")
    void deveClassificarComoNaoConformeSemPlanoTrabalho() {
        PlanoAcaoEspecialDTO planoAcao = criarPlanoAcaoPadrao("CIENTE");

        Adpf854ComplianceResult result = evaluator.avaliar(
                planoAcao,
                Collections.emptyList(),
                Collections.emptyList(),
                hoje
        );

        assertThat(result.status()).isEqualTo(StatusAdpf854.NAO_CONFORME);
        assertThat(result.possuiPlanoTrabalho()).isFalse();
        assertThat(result.inconformidades())
                .extracting(Adpf854ComplianceResult.InconformidadeItem::tipo)
                .contains(TipoInconformidadeAdpf854.AUSENCIA_PLANO_TRABALHO);
    }

    @Test
    @DisplayName("Cenário 3: Deve classificar como NAO_CONFORME quando plano de execução já expirou e falta relatório de gestão")
    void deveClassificarComoNaoConformePlanoExpiradoSemRelatorio() {
        PlanoAcaoEspecialDTO planoAcao = criarPlanoAcaoPadrao("CIENTE");

        // Plano com fim de execução em 2025 (no passado em relação a 2026-09-25)
        PlanoTrabalhoEspecialDTO pt = new PlanoTrabalhoEspecialDTO(
                502L,
                "2024-03-01 10:00:00",
                "APROVADO",
                "Não",
                LocalDate.of(2024, 3, 1),
                LocalDate.of(2025, 6, 30),
                15,
                1001L,
                "44.90.51",
                "NÃO",
                "NÃO",
                null,
                "2024-03-15 14:00:00",
                "Não"
        );

        Adpf854ComplianceResult result = evaluator.avaliar(
                planoAcao,
                List.of(pt),
                Collections.emptyList(),
                hoje
        );

        assertThat(result.status()).isEqualTo(StatusAdpf854.NAO_CONFORME);
        assertThat(result.inconformidades())
                .extracting(Adpf854ComplianceResult.InconformidadeItem::tipo)
                .contains(TipoInconformidadeAdpf854.AUSENCIA_RELATORIO_GESTAO);
    }

    @Test
    @DisplayName("Cenário 4: Deve classificar como ALERTA quando plano está em elaboração e dentro do prazo")
    void deveClassificarComoAlertaPlanoEmElaboracao() {
        PlanoAcaoEspecialDTO planoAcao = criarPlanoAcaoPadrao("CIENTE");

        // Fim de execução no futuro (2027)
        PlanoTrabalhoEspecialDTO pt = new PlanoTrabalhoEspecialDTO(
                503L,
                "2026-01-01 10:00:00",
                "EM_ELABORACAO",
                "Não",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2027, 1, 1),
                12,
                1001L,
                "44.90.51",
                "NÃO",
                "NÃO",
                null,
                null,
                "Sim"
        );

        Adpf854ComplianceResult result = evaluator.avaliar(
                planoAcao,
                List.of(pt),
                Collections.emptyList(),
                hoje
        );

        assertThat(result.status()).isEqualTo(StatusAdpf854.ALERTA);
        assertThat(result.planoTrabalhoAprovado()).isFalse();
        assertThat(result.inconformidades())
                .extracting(Adpf854ComplianceResult.InconformidadeItem::tipo)
                .contains(TipoInconformidadeAdpf854.PLANO_TRABALHO_NAO_APROVADO);
    }

    @Test
    @DisplayName("Cenário 5: Deve classificar como NAO_CONFORME quando situação for IMPEDIDO")
    void deveClassificarComoNaoConformeQuandoImpedido() {
        PlanoAcaoEspecialDTO planoAcao = new PlanoAcaoEspecialDTO(
                1002L,
                "2024.99999",
                2024,
                "TRANSFERENCIA_ESPECIAL",
                "IMPEDIDO",
                "camara@cajazeiras.pb.gov.br",
                null,
                "001",
                1,
                "REGULAR",
                "BANCO DO BRASIL",
                "1111",
                "1",
                "22222",
                "3",
                "DEPUTADO Y",
                2024,
                666,
                2,
                20240002,
                "2024.0002",
                "CUSTEIO",
                "Saúde",
                "Medicamentos",
                "Vedação legal de aplicação",
                new BigDecimal("200000.00"),
                BigDecimal.ZERO,
                "1111-22222",
                2002L,
                302,
                "Aquisição de Remédios",
                "Atenção básica",
                11
        );

        Adpf854ComplianceResult result = evaluator.avaliar(
                planoAcao,
                Collections.emptyList(),
                Collections.emptyList(),
                hoje
        );

        assertThat(result.status()).isEqualTo(StatusAdpf854.NAO_CONFORME);
        assertThat(result.inconformidades())
                .extracting(Adpf854ComplianceResult.InconformidadeItem::tipo)
                .contains(TipoInconformidadeAdpf854.TRANSFERENCIA_IMPEDIDA);
    }
}
