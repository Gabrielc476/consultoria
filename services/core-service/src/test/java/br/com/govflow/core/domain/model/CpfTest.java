package br.com.govflow.core.domain.model;

import br.com.govflow.core.domain.exception.CpfInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class CpfTest {

    @Test
    @DisplayName("Deve criar CPF válido a partir de string com máscara")
    void deveCriarCpfValidoComMascara() {
        // CPF válido gerado para testes algorítmicos
        Cpf cpf = new Cpf("000.000.001-91");
        assertEquals("00000000191", cpf.getValue());
        assertEquals("000.000.001-91", cpf.getFormatted());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "00000000000",
            "11111111111",
            "22222222222",
            "99999999999"
    })
    @DisplayName("Deve rejeitar CPF com dígitos repetidos")
    void deveRejeitarCpfComDigitosRepetidos(String cpfInvalido) {
        assertThrows(CpfInvalidoException.class, () -> new Cpf(cpfInvalido));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "12345678900",
            "11144477734"
    })
    @DisplayName("Deve rejeitar CPF com dígito verificador inválido")
    void deveRejeitarCpfComDvInvalido(String cpfInvalido) {
        assertThrows(CpfInvalidoException.class, () -> new Cpf(cpfInvalido));
    }

    @Test
    @DisplayName("Deve rejeitar CPF nulo ou vazio")
    void deveRejeitarCpfNuloOuVazio() {
        assertThrows(CpfInvalidoException.class, () -> new Cpf(null));
        assertThrows(CpfInvalidoException.class, () -> new Cpf(""));
    }
}
