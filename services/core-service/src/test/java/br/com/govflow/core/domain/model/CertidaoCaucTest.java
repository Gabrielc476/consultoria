package br.com.govflow.core.domain.model;

import br.com.govflow.core.domain.exception.DomainException;
import br.com.govflow.core.domain.exception.TenantInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CertidaoCaucTest {

    private final UUID tenantId = UUID.randomUUID();
    private final UUID prefeituraId = UUID.randomUUID();
    private final LocalDate hoje = LocalDate.of(2026, 9, 25);

    @Test
    @DisplayName("Deve inicializar certidão padrão com 90 dias de validade e status REGULAR")
    void deveInicializarCertidaoPadrao() {
        CertidaoCauc certidao = CertidaoCauc.criarPadrao(
                tenantId,
                prefeituraId,
                TipoExigenciaCauc.REGULARIDADE_FGTS,
                hoje
        );

        assertThat(certidao.getTipoExigencia()).isEqualTo(TipoExigenciaCauc.REGULARIDADE_FGTS);
        assertThat(certidao.getDataEmissao()).isEqualTo(hoje);
        assertThat(certidao.getDataValidade()).isEqualTo(hoje.plusDays(90));
        assertThat(certidao.getDiasParaVencer()).isEqualTo(90);
        assertThat(certidao.getSituacao()).isEqualTo(StatusCertidao.REGULAR);
        assertThat(certidao.isCriticaParaAlerta()).isFalse();
    }

    @Test
    @DisplayName("Deve classificar como ALERTA quando faltarem 10 dias ou menos para o vencimento")
    void deveClassificarComoAlerta() {
        CertidaoCauc certidao = new CertidaoCauc(
                UUID.randomUUID(),
                tenantId,
                prefeituraId,
                TipoExigenciaCauc.RECEITA_FEDERAL_PGFN,
                "CND-12345",
                hoje.minusDays(170),
                hoje.plusDays(10),
                StatusCertidao.REGULAR,
                null,
                null,
                null,
                null
        );

        certidao.reavaliarSituacao(hoje);

        assertThat(certidao.getDiasParaVencer()).isEqualTo(10);
        assertThat(certidao.getSituacao()).isEqualTo(StatusCertidao.ALERTA);
        assertThat(certidao.isCriticaParaAlerta()).isTrue();
    }

    @Test
    @DisplayName("Deve classificar como ALERTA em D-5 e disparar criticidade")
    void deveClassificarComoAlertaEmD5() {
        CertidaoCauc certidao = new CertidaoCauc(
                UUID.randomUUID(),
                tenantId,
                prefeituraId,
                TipoExigenciaCauc.RECEITA_FEDERAL_PGFN,
                "CND-12345",
                hoje.minusDays(175),
                hoje.plusDays(5),
                StatusCertidao.REGULAR,
                null,
                null,
                null,
                null
        );

        certidao.reavaliarSituacao(hoje);

        assertThat(certidao.getDiasParaVencer()).isEqualTo(5);
        assertThat(certidao.getSituacao()).isEqualTo(StatusCertidao.ALERTA);
        assertThat(certidao.isCriticaParaAlerta()).isTrue();
    }

    @Test
    @DisplayName("Deve classificar como VENCIDA quando data de validade for anterior à referência")
    void deveClassificarComoVencida() {
        CertidaoCauc certidao = new CertidaoCauc(
                UUID.randomUUID(),
                tenantId,
                prefeituraId,
                TipoExigenciaCauc.DEBITOS_TRABALHISTAS_CNDT,
                "CNDT-999",
                hoje.minusDays(185),
                hoje.minusDays(2),
                StatusCertidao.REGULAR,
                null,
                null,
                null,
                null
        );

        certidao.reavaliarSituacao(hoje);

        assertThat(certidao.getDiasParaVencer()).isEqualTo(-2);
        assertThat(certidao.getSituacao()).isEqualTo(StatusCertidao.VENCIDA);
        assertThat(certidao.isCriticaParaAlerta()).isTrue();
    }

    @Test
    @DisplayName("Deve rejeitar data de validade anterior à data de emissão")
    void deveRejeitarDataValidadeAnteriorAEmissao() {
        assertThatThrownBy(() -> new CertidaoCauc(
                UUID.randomUUID(),
                tenantId,
                prefeituraId,
                TipoExigenciaCauc.CADIN_FEDERAL,
                "CADIN-01",
                hoje,
                hoje.minusDays(1),
                StatusCertidao.REGULAR,
                null,
                null,
                null,
                null
        )).isInstanceOf(DomainException.class)
                .hasMessageContaining("Data de validade");
    }

    @Test
    @DisplayName("Deve rejeitar tenantId nulo")
    void deveRejeitarTenantIdNulo() {
        assertThatThrownBy(() -> new CertidaoCauc(
                UUID.randomUUID(),
                null,
                prefeituraId,
                TipoExigenciaCauc.CADIN_FEDERAL,
                "CADIN-01",
                hoje,
                hoje.plusDays(30),
                StatusCertidao.REGULAR,
                null,
                null,
                null,
                null
        )).isInstanceOf(TenantInvalidoException.class);
    }
}
