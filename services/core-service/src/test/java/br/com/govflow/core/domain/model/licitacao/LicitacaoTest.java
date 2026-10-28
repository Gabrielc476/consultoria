package br.com.govflow.core.domain.model.licitacao;

import br.com.govflow.core.domain.exception.AioNaoEmitidaException;
import br.com.govflow.core.domain.exception.RegraNegocioLicitacaoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LicitacaoTest {

    private final UUID tenantId = UUID.randomUUID();
    private final UUID convenioId = UUID.randomUUID();
    private final UUID prefeituraId = UUID.randomUUID();

    private Licitacao novaObras() {
        return Licitacao.cadastrar(
                tenantId,
                convenioId,
                prefeituraId,
                "001/2026",
                "Concorrência 002/2026",
                ModalidadeLicitacao.CONCORRENCIA_ELETRONICA,
                CriterioJulgamento.MENOR_PRECO,
                RegimeExecucao.EMPREITADA_PRECO_UNITARIO,
                "Obras civis do Complexo de Saúde",
                new BigDecimal("2000000.00"),
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 20),
                "https://pncp.gov.br/editais/002",
                null,
                null
        );
    }

    private Licitacao homologada() {
        Licitacao licitacao = novaObras();
        licitacao.homologar(
                new BigDecimal("1800000.00"),
                LocalDate.of(2026, 4, 10),
                "12.345.678/0001-90",
                "Construtora Alvorada Ltda"
        );
        return licitacao;
    }

    private Licitacao comVrplAceito() {
        Licitacao licitacao = homologada();
        licitacao.submeterVrpl("VRPL-914250-01", LocalDate.of(2026, 4, 15));
        licitacao.aceitarVrpl("Planilha contratada compatível com a SPA.", LocalDate.of(2026, 5, 2));
        return licitacao;
    }

    @Test
    @DisplayName("Deve cadastrar licitação 1:N em PLANEJAMENTO/EDITAL_PUBLICADO com VRPL e AIO iniciais")
    void deveCadastrarComEstadosIniciais() {
        Licitacao licitacao = novaObras();

        assertThat(licitacao.getConvenioId()).isEqualTo(convenioId);
        assertThat(licitacao.getNumeroLicitacao()).isEqualTo("Concorrência 002/2026");
        assertThat(licitacao.getSituacao()).isEqualTo(SituacaoLicitacao.EDITAL_PUBLICADO);
        assertThat(licitacao.getStatusVrpl()).isEqualTo(StatusVrpl.NAO_ENVIADO);
        assertThat(licitacao.getStatusAio()).isEqualTo(StatusAio.NAO_EMITIDO);
        assertThat(licitacao.podeCriarMedicao()).isFalse();
    }

    @Test
    @DisplayName("Deve calcular EconomiaLicitacao de 10% na homologação")
    void deveCalcularPercentualDeDescontoNaHomologacao() {
        Licitacao licitacao = homologada();

        assertThat(licitacao.getSituacao()).isEqualTo(SituacaoLicitacao.HOMOLOGADA);
        assertThat(licitacao.getPercentualDesconto()).isEqualByComparingTo("10.00");
        assertThat(licitacao.calcularValorEconomia()).isEqualByComparingTo("200000.00");
        assertThat(licitacao.getCnpjVencedor()).isEqualTo("12.345.678/0001-90");
    }

    @Test
    @DisplayName("Deve recusar homologação acima do valor estimado da SPA")
    void deveRecusarHomologacaoAcimaDoEstimado() {
        Licitacao licitacao = novaObras();

        assertThatThrownBy(() -> licitacao.homologar(
                new BigDecimal("2500000.00"),
                LocalDate.of(2026, 4, 10),
                "12.345.678/0001-90",
                "Construtora Alvorada Ltda"
        )).isInstanceOf(RegraNegocioLicitacaoException.class)
                .hasMessageContaining("não pode exceder o valor estimado");
    }

    @Test
    @DisplayName("Deve recusar submissão de VRPL antes da homologação")
    void deveRecusarVrplAntesDaHomologacao() {
        Licitacao licitacao = novaObras();

        assertThatThrownBy(() -> licitacao.submeterVrpl("VRPL-1", LocalDate.now()))
                .isInstanceOf(RegraNegocioLicitacaoException.class)
                .hasMessageContaining("após a homologação municipal");
    }

    @Test
    @DisplayName("Deve transitar VRPL de envio para diligência e depois aceite com parecer técnico")
    void deveRegistrarProtocoloDiligenciaEAceiteDeVrpl() {
        Licitacao licitacao = homologada();
        licitacao.submeterVrpl("VRPL-914250-01", LocalDate.of(2026, 4, 15));
        assertThat(licitacao.getStatusVrpl()).isEqualTo(StatusVrpl.EM_ANALISE_CAIXA);
        assertThat(licitacao.getNumeroVrplTransferegov()).isEqualTo("VRPL-914250-01");

        licitacao.registrarDiligencia("Reapresentar composição de BDI da vencedora.");
        assertThat(licitacao.getStatusVrpl()).isEqualTo(StatusVrpl.DILIGENCIA);

        licitacao.aceitarVrpl("BDI saneado. Planilha homologada.", LocalDate.of(2026, 5, 10));
        assertThat(licitacao.getStatusVrpl()).isEqualTo(StatusVrpl.ACEITO_HOMOLOGADO);
        assertThat(licitacao.getParecerTecnicoVrpl()).contains("BDI saneado");
        assertThat(licitacao.getDataAceiteVrpl()).isEqualTo(LocalDate.of(2026, 5, 10));
    }

    @Test
    @DisplayName("Deve recusar emissão de AIO sem VRPL ACEITO_HOMOLOGADO")
    void deveRecusarAioSemVrplAceito() {
        Licitacao licitacao = homologada();
        licitacao.submeterVrpl("VRPL-914250-01", LocalDate.of(2026, 4, 15));

        assertThatThrownBy(() -> licitacao.emitirAio("AIO-003/2026", LocalDate.of(2026, 5, 20)))
                .isInstanceOf(RegraNegocioLicitacaoException.class)
                .hasMessageContaining("ACEITO_HOMOLOGADO");
    }

    @Test
    @DisplayName("Deve emitir AIO e liberar a TravaAntigrosa para medição")
    void deveEmitirAioELiberarMedicao() {
        Licitacao licitacao = comVrplAceito();
        assertThat(licitacao.podeCriarMedicao()).isFalse();

        licitacao.emitirAio("AIO nº 003/2026 - Caixa GIGOV/JP", LocalDate.of(2026, 5, 20));

        assertThat(licitacao.getStatusAio()).isEqualTo(StatusAio.EMITIDO);
        assertThat(licitacao.getNumeroAio()).contains("003/2026");
        assertThat(licitacao.getDataEmissaoAio()).isEqualTo(LocalDate.of(2026, 5, 20));
        assertThat(licitacao.podeCriarMedicao()).isTrue();
        licitacao.garantirAioEmitidaParaMedicao();
    }

    @Test
    @DisplayName("Deve lançar AioNaoEmitidaException ao tentar criar medição sem AIO")
    void deveBloquearMedicaoSemAio() {
        Licitacao licitacao = comVrplAceito();

        assertThatThrownBy(licitacao::garantirAioEmitidaParaMedicao)
                .isInstanceOf(AioNaoEmitidaException.class)
                .hasMessageContaining("TravaAntigrosa");
    }

    @Test
    @DisplayName("Dois certames independentes podem coexistir no mesmo convênio")
    void devePermitirMultiplosCertamesNoMesmoConvenio() {
        Licitacao obras = novaObras();
        Licitacao supervisao = Licitacao.cadastrar(
                tenantId,
                convenioId,
                prefeituraId,
                "002/2026",
                "Pregão 015/2026",
                ModalidadeLicitacao.PREGAO_ELETRONICO,
                CriterioJulgamento.MENOR_PRECO,
                RegimeExecucao.FORNECIMENTO,
                "Supervisão e fiscalização de obras",
                new BigDecimal("150000.00"),
                null,
                null,
                null,
                null,
                null
        );

        assertThat(obras.getConvenioId()).isEqualTo(supervisao.getConvenioId());
        assertThat(obras.getNumeroLicitacao()).isNotEqualTo(supervisao.getNumeroLicitacao());
        assertThat(supervisao.getSituacao()).isEqualTo(SituacaoLicitacao.PLANEJAMENTO);
    }
}
