package br.com.govflow.core.domain.model;

import br.com.govflow.core.domain.exception.CnpjInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class CnpjTest {

    @Test
    @DisplayName("Deve criar CNPJ válido a partir de string com máscara")
    void deveCriarCnpjValidoComMascara() {
        Cnpj cnpj = new Cnpj("08.778.326/0001-56"); // PM João Pessoa
        assertEquals("08778326000156", cnpj.getValue());
        assertEquals("08.778.326/0001-56", cnpj.getFormatted());
        assertTrue(cnpj.isMatriz());
    }

    @Test
    @DisplayName("Deve criar CNPJ válido a partir de string sem máscara")
    void deveCriarCnpjValidoSemMascara() {
        Cnpj cnpj = new Cnpj("13519354000199"); // Consultoria Um / SME
        assertEquals("13519354000199", cnpj.getValue());
        assertEquals("13.519.354/0001-99", cnpj.getFormatted());
        assertTrue(cnpj.isMatriz());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "00000000000000",
            "11111111111111",
            "22222222222222",
            "33333333333333",
            "99999999999999"
    })
    @DisplayName("Deve rejeitar CNPJ com todos os dígitos idênticos")
    void deveRejeitarCnpjComDigitosIdenticos(String cnpjInvalido) {
        assertThrows(CnpjInvalidoException.class, () -> new Cnpj(cnpjInvalido));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "08.778.326/0001-57", // DV incorreto
            "13.519.354/0001-00", // DV incorreto
            "12.345.678/0001-90"  // DV incorreto
    })
    @DisplayName("Deve rejeitar CNPJ com dígitos verificadores inválidos")
    void deveRejeitarCnpjComDvInvalido(String cnpjInvalido) {
        assertThrows(CnpjInvalidoException.class, () -> new Cnpj(cnpjInvalido));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "123",
            "1234567890123",   // 13 dígitos
            "123456789012345"  // 15 dígitos
    })
    @DisplayName("Deve rejeitar CNPJ com tamanho diferente de 14 dígitos")
    void deveRejeitarCnpjComTamanhoInvalido(String cnpjInvalido) {
        assertThrows(CnpjInvalidoException.class, () -> new Cnpj(cnpjInvalido));
    }

    @Test
    @DisplayName("Deve rejeitar CNPJ nulo ou vazio")
    void deveRejeitarCnpjNuloOuVazio() {
        assertThrows(CnpjInvalidoException.class, () -> new Cnpj(null));
        assertThrows(CnpjInvalidoException.class, () -> new Cnpj("   "));
    }
}
