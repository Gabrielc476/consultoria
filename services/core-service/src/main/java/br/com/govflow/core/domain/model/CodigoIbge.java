package br.com.govflow.core.domain.model;

import br.com.govflow.core.domain.exception.CodigoIbgeInvalidoException;

import java.io.Serializable;
import java.util.Objects;

public final class CodigoIbge implements Serializable {

    private static final int IBGE_LENGTH = 7;
    private static final int[] WEIGHTS = {1, 2, 1, 2, 1, 2};

    private final String value;

    public CodigoIbge(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new CodigoIbgeInvalidoException("Código IBGE não pode ser nulo ou vazio.");
        }

        String cleanValue = rawValue.replaceAll("\\D", "");

        if (cleanValue.length() != IBGE_LENGTH) {
            throw new CodigoIbgeInvalidoException("Código IBGE deve conter exatamente 7 dígitos numéricos.");
        }

        if (!hasValidCheckDigit(cleanValue)) {
            throw new CodigoIbgeInvalidoException("Dígito verificador do código IBGE inválido.");
        }

        this.value = cleanValue;
    }

    public String getValue() {
        return value;
    }

    public String getPrefixoUf() {
        return value.substring(0, 2);
    }

    public void validarCompatibilidadeUf(Uf uf) {
        if (uf == null) {
            throw new CodigoIbgeInvalidoException("UF não pode ser nula para validação de compatibilidade com o código IBGE.");
        }
        if (!getPrefixoUf().equals(uf.getCodigoIbgeUf())) {
            throw new CodigoIbgeInvalidoException(String.format(
                    "Código IBGE %s é incompatível com a UF %s (prefixo esperado: %s, prefixo informado: %s).",
                    value, uf.name(), uf.getCodigoIbgeUf(), getPrefixoUf()));
        }
    }

    private static boolean hasValidCheckDigit(String text) {
        int sum = 0;
        for (int i = 0; i < 6; i++) {
            int digit = Character.getNumericValue(text.charAt(i));
            int product = digit * WEIGHTS[i];
            sum += (product < 10) ? product : (product / 10 + product % 10);
        }

        int remainder = sum % 10;
        int expectedDv = (10 - remainder) % 10;
        int actualDv = Character.getNumericValue(text.charAt(6));

        return actualDv == expectedDv;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CodigoIbge that = (CodigoIbge) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
