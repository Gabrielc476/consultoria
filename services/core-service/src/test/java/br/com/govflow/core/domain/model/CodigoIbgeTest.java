package br.com.govflow.core.domain.model;

import br.com.govflow.core.domain.exception.CodigoIbgeInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class CodigoIbgeTest {

    @ParameterizedTest
    @CsvSource({
            "2512101, PB", // Pombal - PB
            "2507507, PB", // João Pessoa - PB
            "2509701, PB", // Massaranduba - PB
            "2504009, PB", // Campina Grande - PB
            "3550308, SP", // São Paulo - SP
            "3304557, RJ", // Rio de Janeiro - RJ
            "3106200, MG", // Belo Horizonte - MG
            "5300108, DF"  // Brasília - DF
    })
    @DisplayName("Deve validar com sucesso códigos IBGE oficiais e compatíveis com a UF")
    void deveAceitarCodigosIbgeOficiaisValidos(String codigo, String ufStr) {
        CodigoIbge ibge = new CodigoIbge(codigo);
        assertEquals(codigo, ibge.getValue());

        Uf uf = Uf.valueOf(ufStr);
        assertDoesNotThrow(() -> ibge.validarCompatibilidadeUf(uf));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "2512109", // DV esperado é 1
            "2507500", // DV esperado é 7
            "3550300"  // DV esperado é 8
    })
    @DisplayName("Deve rejeitar código IBGE com dígito verificador incorreto")
    void deveRejeitarCodigoIbgeComDvIncorreto(String codigoInvalido) {
        assertThrows(CodigoIbgeInvalidoException.class, () -> new CodigoIbge(codigoInvalido));
    }

    @Test
    @DisplayName("Deve rejeitar código IBGE quando for incompatível com a UF")
    void deveRejeitarIncompatibilidadeDeUf() {
        CodigoIbge ibgePombalPb = new CodigoIbge("2512101");

        // Prefixo de Pombal é 25 (PB), deve falhar ao validar com SP (35) ou PE (26)
        assertThrows(CodigoIbgeInvalidoException.class, () -> ibgePombalPb.validarCompatibilidadeUf(Uf.SP));
        assertThrows(CodigoIbgeInvalidoException.class, () -> ibgePombalPb.validarCompatibilidadeUf(Uf.PE));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "251210",    // 6 dígitos
            "25121011",  // 8 dígitos
            "123"
    })
    @DisplayName("Deve rejeitar código IBGE com tamanho diferente de 7 dígitos")
    void deveRejeitarTamanhoInvalido(String codigoInvalido) {
        assertThrows(CodigoIbgeInvalidoException.class, () -> new CodigoIbge(codigoInvalido));
    }

    @Test
    @DisplayName("Deve rejeitar código IBGE nulo ou em branco")
    void deveRejeitarNuloOuBranco() {
        assertThrows(CodigoIbgeInvalidoException.class, () -> new CodigoIbge(null));
        assertThrows(CodigoIbgeInvalidoException.class, () -> new CodigoIbge("   "));
    }
}
