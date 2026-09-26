package br.com.govflow.core.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TipoExigenciaCaucTest {

    @Test
    @DisplayName("Deve conter exatamente as 16 exigências oficiais do CAUC")
    void deveConterExatamenteDezesseisExigencias() {
        assertThat(TipoExigenciaCauc.values()).hasSize(16);
    }

    @Test
    @DisplayName("Deve distribuir as 16 exigências igualmente nos 4 grupos legais (4 por grupo)")
    void deveDistribuirEmQuatroGruposLegais() {
        for (GrupoCauc grupo : GrupoCauc.values()) {
            List<TipoExigenciaCauc> itensDoGrupo = Arrays.stream(TipoExigenciaCauc.values())
                    .filter(item -> item.getGrupo() == grupo)
                    .toList();

            assertThat(itensDoGrupo)
                    .as("Grupo %s deve possuir exatamente 4 exigências", grupo)
                    .hasSize(4);
        }
    }

    @Test
    @DisplayName("Deve mapear códigos 1.1 até 4.4 corretamente via fromCodigo")
    void deveMapearCodigosCorretamente() {
        assertThat(TipoExigenciaCauc.fromCodigo("1.1")).contains(TipoExigenciaCauc.RECEITA_FEDERAL_PGFN);
        assertThat(TipoExigenciaCauc.fromCodigo("1.2")).contains(TipoExigenciaCauc.REGULARIDADE_FGTS);
        assertThat(TipoExigenciaCauc.fromCodigo("1.3")).contains(TipoExigenciaCauc.REGULARIDADE_PREVIDENCIARIA);
        assertThat(TipoExigenciaCauc.fromCodigo("1.4")).contains(TipoExigenciaCauc.DEBITOS_TRABALHISTAS_CNDT);

        assertThat(TipoExigenciaCauc.fromCodigo("2.1")).contains(TipoExigenciaCauc.PRESTACAO_CONTAS_RECURSOS_FEDERAIS);
        assertThat(TipoExigenciaCauc.fromCodigo("2.2")).contains(TipoExigenciaCauc.CADIN_FEDERAL);
        assertThat(TipoExigenciaCauc.fromCodigo("2.3")).contains(TipoExigenciaCauc.PRESTACAO_CONTAS_FNDE_FNS);
        assertThat(TipoExigenciaCauc.fromCodigo("2.4")).contains(TipoExigenciaCauc.PRECATORIOS_JUDICIAIS);

        assertThat(TipoExigenciaCauc.fromCodigo("3.1")).contains(TipoExigenciaCauc.RREO_SICONFI);
        assertThat(TipoExigenciaCauc.fromCodigo("3.2")).contains(TipoExigenciaCauc.RGF_SICONFI);
        assertThat(TipoExigenciaCauc.fromCodigo("3.3")).contains(TipoExigenciaCauc.BALANCO_ANUAL_SICONFI);
        assertThat(TipoExigenciaCauc.fromCodigo("3.4")).contains(TipoExigenciaCauc.CONTAS_ANUAIS_TCE);

        assertThat(TipoExigenciaCauc.fromCodigo("4.1")).contains(TipoExigenciaCauc.APLICACAO_SAUDE_SIOPS);
        assertThat(TipoExigenciaCauc.fromCodigo("4.2")).contains(TipoExigenciaCauc.APLICACAO_EDUCACAO_SIOPE);
        assertThat(TipoExigenciaCauc.fromCodigo("4.3")).contains(TipoExigenciaCauc.DESPESA_PESSOAL_LRF);
        assertThat(TipoExigenciaCauc.fromCodigo("4.4")).contains(TipoExigenciaCauc.DIVIDA_CONSOLIDADA_CREDITO);

        assertThat(TipoExigenciaCauc.fromCodigo("invalido")).isEmpty();
        assertThat(TipoExigenciaCauc.fromCodigo(null)).isEmpty();
    }
}
