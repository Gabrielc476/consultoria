package br.com.govflow.core.domain.model;

import br.com.govflow.core.domain.exception.CnpjInvalidoException;

import java.io.Serializable;
import java.util.Objects;

public final class Cnpj implements Serializable {

    private static final int CNPJ_LENGTH = 14;
    private static final int[] WEIGHTS_FIRST_DIGIT = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] WEIGHTS_SECOND_DIGIT = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private final String value;

    public Cnpj(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new CnpjInvalidoException("CNPJ não pode ser nulo ou vazio.");
        }

        String cleanValue = rawValue.replaceAll("\\D", "");

        if (cleanValue.length() != CNPJ_LENGTH) {
            throw new CnpjInvalidoException("CNPJ deve conter exatamente 14 dígitos numéricos.");
        }

        if (hasAllIdenticalDigits(cleanValue)) {
            throw new CnpjInvalidoException("CNPJ não pode ser composto por dígitos idênticos repetidos.");
        }

        if (!hasValidCheckDigits(cleanValue)) {
            throw new CnpjInvalidoException("Dígitos verificadores do CNPJ são inválidos.");
        }

        this.value = cleanValue;
    }

    public String getValue() {
        return value;
    }

    public String getFormatted() {
        return String.format("%s.%s.%s/%s-%s",
                value.substring(0, 2),
                value.substring(2, 5),
                value.substring(5, 8),
                value.substring(8, 12),
                value.substring(12, 14));
    }

    public boolean isMatriz() {
        return value.startsWith("0001", 8);
    }

    private static boolean hasAllIdenticalDigits(String text) {
        char firstChar = text.charAt(0);
        for (int i = 1; i < text.length(); i++) {
            if (text.charAt(i) != firstChar) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasValidCheckDigits(String text) {
        int firstDigit = calculateDigit(text.substring(0, 12), WEIGHTS_FIRST_DIGIT);
        int secondDigit = calculateDigit(text.substring(0, 12) + firstDigit, WEIGHTS_SECOND_DIGIT);

        return Character.getNumericValue(text.charAt(12)) == firstDigit
                && Character.getNumericValue(text.charAt(13)) == secondDigit;
    }

    private static int calculateDigit(String base, int[] weights) {
        int sum = 0;
        for (int i = 0; i < base.length(); i++) {
            sum += Character.getNumericValue(base.charAt(i)) * weights[i];
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cnpj cnpj = (Cnpj) o;
        return Objects.equals(value, cnpj.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return getFormatted();
    }
}
