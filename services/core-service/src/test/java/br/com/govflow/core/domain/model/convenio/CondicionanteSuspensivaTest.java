package br.com.govflow.core.domain.model.convenio;

import br.com.govflow.core.domain.exception.RegraNegocioClausulaSuspensivaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CondicionanteSuspensivaTest {

    private final UUID tenantId = UUID.randomUUID();
    private final UUID convenioId = UUID.randomUUID();

    @Test
    @DisplayName("Deve inicializar nova condicionante suspensiva com status PENDENTE")
    void deveInicializarComStatusPendente() {
        var cond = CondicionanteSuspensiva.nova(tenantId, convenioId, TipoCondicionanteSuspensiva.ENGENHARIA_PROJETOS_SINAPI);

        assertThat(cond.getId()).isNotNull();
        assertThat(cond.getTenantId()).isEqualTo(tenantId);
        assertThat(cond.getConvenioId()).isEqualTo(convenioId);
        assertThat(cond.getTipoCondicionante()).isEqualTo(TipoCondicionanteSuspensiva.ENGENHARIA_PROJETOS_SINAPI);
        assertThat(cond.getStatus()).isEqualTo(StatusCondicionanteSuspensiva.PENDENTE);
        assertThat(cond.isAprovado()).isFalse();
    }

    @Test
    @DisplayName("Deve submeter para análise da Caixa alterando status para EM_ANALISE_CAIXA")
    void deveSubmeterParaAnaliseCaixa() {
        var cond = CondicionanteSuspensiva.nova(tenantId, convenioId, TipoCondicionanteSuspensiva.LICENCIAMENTO_AMBIENTAL);
        cond.submeterParaAnaliseCaixa();

        assertThat(cond.getStatus()).isEqualTo(StatusCondicionanteSuspensiva.EM_ANALISE_CAIXA);
    }

    @Test
    @DisplayName("Não deve permitir submeter para análise condicionante já aprovada")
    void naoDeveSubmeterSeJaAprovada() {
        var cond = CondicionanteSuspensiva.nova(tenantId, convenioId, TipoCondicionanteSuspensiva.TITULARIDADE_IMOVEL);
        cond.aprovar("CRI-12345", LocalDate.now(), LocalDate.now().plusYears(1), null, null, null, "1º CRI Patos", "s3/cri.pdf");

        assertThatThrownBy(cond::submeterParaAnaliseCaixa)
                .isInstanceOf(RegraNegocioClausulaSuspensivaException.class)
                .hasMessageContaining("já se encontra aprovada");
    }

    @Test
    @DisplayName("Deve registrar notificação de diligência com prazo de saneamento")
    void deveRegistrarDiligenciaComSucesso() {
        var cond = CondicionanteSuspensiva.nova(tenantId, convenioId, TipoCondicionanteSuspensiva.ENGENHARIA_PROJETOS_SINAPI);
        LocalDate hoje = LocalDate.of(2026, 9, 26);
        LocalDate prazoSaneamento = hoje.plusDays(15);

        cond.registrarDiligencia("Adequar Curva ABC ao SINAPI vigente", "s3/laudo-pendencias.pdf", prazoSaneamento, hoje);

        assertThat(cond.getStatus()).isEqualTo(StatusCondicionanteSuspensiva.DILIGENCIA_EMITIDA);
        assertThat(cond.getObservacoesAnaliseCaixa()).isEqualTo("Adequar Curva ABC ao SINAPI vigente");
        assertThat(cond.getS3KeyLaudoPendencias()).isEqualTo("s3/laudo-pendencias.pdf");
        assertThat(cond.getDataLimiteSaneamento()).isEqualTo(prazoSaneamento);
    }

    @Test
    @DisplayName("Deve rejeitar data limite de saneamento retroativa")
    void deveRejeitarDataLimiteRetroativa() {
        var cond = CondicionanteSuspensiva.nova(tenantId, convenioId, TipoCondicionanteSuspensiva.ENGENHARIA_PROJETOS_SINAPI);
        LocalDate hoje = LocalDate.of(2026, 9, 26);
        LocalDate prazoPassado = hoje.minusDays(1);

        assertThatThrownBy(() -> cond.registrarDiligencia("Pendência", "s3/laudo.pdf", prazoPassado, hoje))
                .isInstanceOf(RegraNegocioClausulaSuspensivaException.class)
                .hasMessageContaining("não pode ser anterior à data atual");
    }

    @Test
    @DisplayName("Deve aprovar pilar de engenharia com parâmetros da SPA e BDI do TCU")
    void deveAprovarComParametrosTecnicosCompletos() {
        var cond = CondicionanteSuspensiva.nova(tenantId, convenioId, TipoCondicionanteSuspensiva.ENGENHARIA_PROJETOS_SINAPI);
        BigDecimal orcamento = new BigDecimal("2050000.00");
        BigDecimal bdi = new BigDecimal("22.50");

        cond.aprovar("SPA-914250/2026", LocalDate.of(2026, 9, 26), null, orcamento, bdi, "ART-PB-88421", "Caixa GIGOV", "s3/lae_spa.pdf");

        assertThat(cond.isAprovado()).isTrue();
        assertThat(cond.getStatus()).isEqualTo(StatusCondicionanteSuspensiva.APROVADO);
        assertThat(cond.getNumeroDocumentoComprobatorio()).isEqualTo("SPA-914250/2026");
        assertThat(cond.getValorOrcamentoAprovadoCaixa()).isEqualByComparingTo(orcamento);
        assertThat(cond.getPercentualBdiAprovado()).isEqualByComparingTo(bdi);
        assertThat(cond.getNumeroArtRrt()).isEqualTo("ART-PB-88421");
        assertThat(cond.getOrgaoEmissor()).isEqualTo("Caixa GIGOV");
        assertThat(cond.getS3KeyDocumento()).isEqualTo("s3/lae_spa.pdf");
    }

    @Test
    @DisplayName("Não deve aprovar condicionante sem informar o número do documento comprobatório")
    void deveRejeitarAprovacaoSemNumeroDocumento() {
        var cond = CondicionanteSuspensiva.nova(tenantId, convenioId, TipoCondicionanteSuspensiva.LICENCIAMENTO_AMBIENTAL);

        assertThatThrownBy(() -> cond.aprovar("   ", LocalDate.now(), null, null, null, null, null, null))
                .isInstanceOf(RegraNegocioClausulaSuspensivaException.class)
                .hasMessageContaining("número do documento comprobatório oficial é obrigatório");
    }

    @Test
    @DisplayName("Não deve aprovar condicionante com BDI negativo")
    void deveRejeitarBdiNegativo() {
        var cond = CondicionanteSuspensiva.nova(tenantId, convenioId, TipoCondicionanteSuspensiva.ENGENHARIA_PROJETOS_SINAPI);

        assertThatThrownBy(() -> cond.aprovar("SPA-01", LocalDate.now(), null, null, new BigDecimal("-5.00"), null, null, null))
                .isInstanceOf(RegraNegocioClausulaSuspensivaException.class)
                .hasMessageContaining("percentual de BDI não pode ser negativo");
    }
}
