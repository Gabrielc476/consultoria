package br.com.govflow.transferegov.domain.radar;

import br.com.govflow.transferegov.persistence.entity.SincronizacaoConvenioEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Testes Unitários da Régua de Criticidade (DeadlineDetectorService)")
class DeadlineDetectorServiceTest {

    private DeadlineDetectorService service;
    private final LocalDate dataReferencia = LocalDate.of(2026, 9, 24);

    @BeforeEach
    void setUp() {
        service = new DeadlineDetectorService();
    }

    private SincronizacaoConvenioEntity criarConvenioBase() {
        SincronizacaoConvenioEntity c = new SincronizacaoConvenioEntity();
        c.setId(UUID.randomUUID());
        c.setNrConvenio("912345/2024");
        c.setIdProposta("054321/2024");
        c.setMunicipio("Massaranduba");
        c.setUf("PB");
        c.setCnpjProponente("08847784000144");
        c.setNomeProponente("PREFEITURA MUNICIPAL DE MASSARANDUBA");
        c.setSituacaoConvenio("EM_EXECUCAO");
        c.setInstrumentoAtivo(true);
        c.setObjeto("Pavimentação e drenagem de vias públicas");
        c.setValorGlobal(new BigDecimal("500000.00"));
        c.setValorRepasse(new BigDecimal("450000.00"));
        return c;
    }

    @Test
    @DisplayName("Deve classificar como CRITICO convênio com prazo vencido")
    void deveClassificarComoCriticoQuandoVencido() {
        SincronizacaoConvenioEntity convenio = criarConvenioBase();
        // Vencido há 5 dias em relação a 2026-09-24
        convenio.setDataFimVigencia(dataReferencia.minusDays(5));

        AlertaConvenioDTO alerta = service.avaliar(convenio, dataReferencia);

        assertThat(alerta.nivelRisco()).isEqualTo(NivelRisco.CRITICO);
        assertThat(alerta.diasRestantes()).isEqualTo(-5L);
        assertThat(alerta.tipoPrazoMaisProximo()).isEqualTo(TipoPrazo.FIM_VIGENCIA);
        assertThat(alerta.prazoMaisProximo()).isEqualTo(dataReferencia.minusDays(5));
    }

    @Test
    @DisplayName("Deve classificar como CRITICO convênio com menos de 15 dias restantes")
    void deveClassificarComoCriticoQuandoRestamMenosDe15Dias() {
        SincronizacaoConvenioEntity convenio = criarConvenioBase();
        // Vence em 10 dias
        convenio.setDataFimVigencia(dataReferencia.plusDays(10));

        AlertaConvenioDTO alerta = service.avaliar(convenio, dataReferencia);

        assertThat(alerta.nivelRisco()).isEqualTo(NivelRisco.CRITICO);
        assertThat(alerta.diasRestantes()).isEqualTo(10L);
        assertThat(alerta.tipoPrazoMaisProximo()).isEqualTo(TipoPrazo.FIM_VIGENCIA);
    }

    @Test
    @DisplayName("Deve classificar como ATENCAO convênio com 16 a 60 dias restantes")
    void deveClassificarComoAtencaoQuandoRestamEntre16e60Dias() {
        SincronizacaoConvenioEntity convenio = criarConvenioBase();
        // Vence em 45 dias
        convenio.setDataFimVigencia(dataReferencia.plusDays(45));

        AlertaConvenioDTO alerta = service.avaliar(convenio, dataReferencia);

        assertThat(alerta.nivelRisco()).isEqualTo(NivelRisco.ATENCAO);
        assertThat(alerta.diasRestantes()).isEqualTo(45L);
        assertThat(alerta.tipoPrazoMaisProximo()).isEqualTo(TipoPrazo.FIM_VIGENCIA);
    }

    @Test
    @DisplayName("Deve classificar como REGULAR convênio com mais de 60 dias restantes")
    void deveClassificarComoRegularQuandoRestamMaisDe60Dias() {
        SincronizacaoConvenioEntity convenio = criarConvenioBase();
        // Vence em 90 dias
        convenio.setDataFimVigencia(dataReferencia.plusDays(90));

        AlertaConvenioDTO alerta = service.avaliar(convenio, dataReferencia);

        assertThat(alerta.nivelRisco()).isEqualTo(NivelRisco.REGULAR);
        assertThat(alerta.diasRestantes()).isEqualTo(90L);
    }

    @Test
    @DisplayName("Deve priorizar prazo de maior urgência entre múltiplas datas ativas")
    void devePriorizarPrazoMaisCriticoEntreMultiplasDatas() {
        SincronizacaoConvenioEntity convenio = criarConvenioBase();
        // Cláusula suspensiva iminente (8 dias -> CRITICO)
        convenio.setDataSuspensiva(dataReferencia.plusDays(8));
        // Vigência distante (150 dias -> REGULAR)
        convenio.setDataFimVigencia(dataReferencia.plusDays(150));
        // Prestação de contas (210 dias -> REGULAR)
        convenio.setDataLimitePrestacaoContas(dataReferencia.plusDays(210));

        AlertaConvenioDTO alerta = service.avaliar(convenio, dataReferencia);

        assertThat(alerta.nivelRisco()).isEqualTo(NivelRisco.CRITICO);
        assertThat(alerta.tipoPrazoMaisProximo()).isEqualTo(TipoPrazo.CLAUSULA_SUSPENSIVA);
        assertThat(alerta.prazoMaisProximo()).isEqualTo(dataReferencia.plusDays(8));
        assertThat(alerta.diasRestantes()).isEqualTo(8L);
        assertThat(alerta.diasFimVigencia()).isEqualTo(150L);
        assertThat(alerta.diasPrestacaoContas()).isEqualTo(210L);
    }

    @Test
    @DisplayName("Deve classificar como REGULAR convênio sem datas de prazo preenchidas")
    void deveTratarConvenioSemPrazosDefinidos() {
        SincronizacaoConvenioEntity convenio = criarConvenioBase();
        convenio.setDataFimVigencia(null);
        convenio.setDataSuspensiva(null);
        convenio.setDataLimitePrestacaoContas(null);

        AlertaConvenioDTO alerta = service.avaliar(convenio, dataReferencia);

        assertThat(alerta.nivelRisco()).isEqualTo(NivelRisco.REGULAR);
        assertThat(alerta.diasRestantes()).isNull();
        assertThat(alerta.tipoPrazoMaisProximo()).isNull();
        assertThat(alerta.prazoMaisProximo()).isNull();
    }
}
