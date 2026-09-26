package br.com.govflow.core.domain.model.convenio;

import br.com.govflow.core.domain.exception.ClausulaSuspensivaNaoPodeSerSuperadaException;
import br.com.govflow.core.domain.exception.RegraNegocioClausulaSuspensivaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConvenioTest {

    private final UUID tenantId = UUID.randomUUID();
    private final UUID prefeituraId = UUID.randomUUID();
    private final UUID convenioId = UUID.randomUUID();
    private LocalDate dataReferencia;

    @BeforeEach
    void setUp() {
        dataReferencia = LocalDate.of(2026, 9, 26);
    }

    private Convenio criarConvenio(boolean possuiSuspensiva, LocalDate prazoSuspensiva) {
        return new Convenio(
                convenioId,
                tenantId,
                prefeituraId,
                "914250/2023",
                "00124/2023",
                "FNDE / MEC",
                "Construção de Creche Proinfância Tipo 2",
                new BigDecimal("2050000.00"),
                new BigDecimal("1850000.00"),
                new BigDecimal("200000.00"),
                "EM_EXECUCAO",
                possuiSuspensiva,
                prazoSuspensiva,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2027, 1, 1),
                false,
                null,
                null,
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    @DisplayName("Deve calcular dias restantes e semáforo REGULAR quando prazo for superior a 90 dias")
    void deveCalcularSemaforoRegular() {
        LocalDate prazoFatal = dataReferencia.plusDays(120);
        var convenio = criarConvenio(true, prazoFatal);

        assertThat(convenio.calcularDiasRestantes(dataReferencia)).isEqualTo(120);
        assertThat(convenio.calcularCriticidadePrazo(dataReferencia)).isEqualTo(CriticidadePrazoSuspensiva.REGULAR);
    }

    @Test
    @DisplayName("Deve calcular semáforo ATENCAO quando prazo estiver entre 31 e 90 dias")
    void deveCalcularSemaforoAtencao() {
        LocalDate prazoFatal = dataReferencia.plusDays(45);
        var convenio = criarConvenio(true, prazoFatal);

        assertThat(convenio.calcularDiasRestantes(dataReferencia)).isEqualTo(45);
        assertThat(convenio.calcularCriticidadePrazo(dataReferencia)).isEqualTo(CriticidadePrazoSuspensiva.ATENCAO);
    }

    @Test
    @DisplayName("Deve calcular semáforo CRITICO quando prazo for menor ou igual a 30 dias")
    void deveCalcularSemaforoCritico() {
        LocalDate prazoFatal = dataReferencia.plusDays(20);
        var convenio = criarConvenio(true, prazoFatal);

        assertThat(convenio.calcularDiasRestantes(dataReferencia)).isEqualTo(20);
        assertThat(convenio.calcularCriticidadePrazo(dataReferencia)).isEqualTo(CriticidadePrazoSuspensiva.CRITICO);
    }

    @Test
    @DisplayName("Deve calcular semáforo EXPIRADO quando prazo fatal já venceu")
    void deveCalcularSemaforoExpirado() {
        LocalDate prazoFatal = dataReferencia.minusDays(5);
        var convenio = criarConvenio(true, prazoFatal);

        assertThat(convenio.calcularDiasRestantes(dataReferencia)).isEqualTo(-5);
        assertThat(convenio.calcularCriticidadePrazo(dataReferencia)).isEqualTo(CriticidadePrazoSuspensiva.EXPIRADO);
    }

    @Test
    @DisplayName("Deve considerar o novo prazo prorrogado no cálculo de dias restantes")
    void deveConsiderarPrazoProrrogado() {
        LocalDate prazoOriginal = dataReferencia.plusDays(10);
        LocalDate prazoProrrogado = dataReferencia.plusDays(100);
        var convenio = criarConvenio(true, prazoOriginal);

        convenio.solicitarProrrogacaoPrazo(prazoProrrogado, dataReferencia);

        assertThat(convenio.isProrrogacaoSolicitada()).isTrue();
        assertThat(convenio.getNovoPrazoProrrogado()).isEqualTo(prazoProrrogado);
        assertThat(convenio.getPrazoFatalEfetivo()).isEqualTo(prazoProrrogado);
        assertThat(convenio.calcularDiasRestantes(dataReferencia)).isEqualTo(100);
        assertThat(convenio.calcularCriticidadePrazo(dataReferencia)).isEqualTo(CriticidadePrazoSuspensiva.REGULAR);
    }

    @Test
    @DisplayName("Não deve permitir solicitar prorrogação com data retroativa ou anterior ao prazo atual")
    void deveRejeitarProrrogacaoInvalida() {
        LocalDate prazoOriginal = dataReferencia.plusDays(30);
        var convenio = criarConvenio(true, prazoOriginal);

        assertThatThrownBy(() -> convenio.solicitarProrrogacaoPrazo(dataReferencia.plusDays(20), dataReferencia))
                .isInstanceOf(RegraNegocioClausulaSuspensivaException.class)
                .hasMessageContaining("deve ser posterior ao prazo fatal atual");
    }

    @Test
    @DisplayName("Deve superar a Cláusula Suspensiva quando todos os três pilares estiverem aprovados")
    void deveSuperarClausulaSuspensivaComSucesso() {
        var convenio = criarConvenio(true, dataReferencia.plusDays(60));

        var condEng = CondicionanteSuspensiva.nova(tenantId, convenioId, TipoCondicionanteSuspensiva.ENGENHARIA_PROJETOS_SINAPI);
        condEng.aprovar("SPA-01", LocalDate.now(), null, new BigDecimal("2000000"), new BigDecimal("22"), "ART-01", "GIGOV", "s3/eng.pdf");

        var condAmb = CondicionanteSuspensiva.nova(tenantId, convenioId, TipoCondicionanteSuspensiva.LICENCIAMENTO_AMBIENTAL);
        condAmb.aprovar("LI-2026/04", LocalDate.now(), LocalDate.now().plusYears(2), null, null, null, "SUDEMA", "s3/amb.pdf");

        var condTit = CondicionanteSuspensiva.nova(tenantId, convenioId, TipoCondicionanteSuspensiva.TITULARIDADE_IMOVEL);
        condTit.aprovar("CRI-4819", LocalDate.now(), LocalDate.now().plusDays(90), null, null, null, "CRI Patos", "s3/cri.pdf");

        List<CondicionanteSuspensiva> pilares = List.of(condEng, condAmb, condTit);

        convenio.superarClausulaSuspensiva("s3/termo_retirada_914250.pdf", pilares);

        assertThat(convenio.isClausulaSuspensivaSuperada()).isTrue();
        assertThat(convenio.getS3KeyTermoRetiradaSuspensiva()).isEqualTo("s3/termo_retirada_914250.pdf");
    }

    @Test
    @DisplayName("Deve bloquear a superação da Cláusula Suspensiva se algum dos 3 pilares não estiver aprovado")
    void deveBloquearSuperacaoComPilaresPendentes() {
        var convenio = criarConvenio(true, dataReferencia.plusDays(60));

        var condEng = CondicionanteSuspensiva.nova(tenantId, convenioId, TipoCondicionanteSuspensiva.ENGENHARIA_PROJETOS_SINAPI);
        condEng.aprovar("SPA-01", LocalDate.now(), null, new BigDecimal("2000000"), new BigDecimal("22"), "ART-01", "GIGOV", "s3/eng.pdf");

        var condAmb = CondicionanteSuspensiva.nova(tenantId, convenioId, TipoCondicionanteSuspensiva.LICENCIAMENTO_AMBIENTAL);
        // Ambiente ainda PENDENTE!

        var condTit = CondicionanteSuspensiva.nova(tenantId, convenioId, TipoCondicionanteSuspensiva.TITULARIDADE_IMOVEL);
        condTit.aprovar("CRI-4819", LocalDate.now(), LocalDate.now().plusDays(90), null, null, null, "CRI Patos", "s3/cri.pdf");

        List<CondicionanteSuspensiva> pilares = List.of(condEng, condAmb, condTit);

        assertThatThrownBy(() -> convenio.superarClausulaSuspensiva("s3/termo.pdf", pilares))
                .isInstanceOf(ClausulaSuspensivaNaoPodeSerSuperadaException.class)
                .hasMessageContaining("Licenciamento Ambiental");
    }
}
