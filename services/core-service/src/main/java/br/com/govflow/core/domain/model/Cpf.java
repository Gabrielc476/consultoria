package br.com.govflow.core.domain.model;

import br.com.govflow.core.domain.exception.CpfInvalidoException;

import java.io.Serializable;
import java.util.Objects;

public final class Cpf implements Serializable {

    private static final int CPF_LENGTH = 11;
    private static final int[] WEIGHTS_FIRST_DIGIT = {10, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] WEIGHTS_SECOND_DIGIT = {11, 10, 9, 8, 7, 6, 5, 4, 3, 2};

    private final String value;

    public Cpf(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new CpfInvalidoException("CPF não pode ser nulo ou vazio.");
        }

        String cleanValue = rawValue.replaceAll("\\D", "");

        if (cleanValue.length() != CPF_LENGTH) {
            throw new CpfInvalidoException("CPF deve conter exatamente 11 dígitos numéricos.");
        }

        if (hasAllIdenticalDigits(cleanValue)) {
            throw new CpfInvalidoException("CPF não pode ser composto por dígitos idênticos repetidos.");
        }

        if (!hasValidCheckDigits(cleanValue)) {
            throw new CpfInvalidoException("Dígitos verificadores do CPF são inválidos.");
        }

        this.value = cleanValue;
    }

    public String getValue() {
        return value;
    }

    public String getFormatted() {
        return String.format("%s.%s.%s-%s",
                value.substring(0, 3),
                value.substring(3, 6),
                value.substring(6, 9),
                value.substring(9, 11));
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
        int firstDigit = calculateDigit(text.substring(0, 9), WEIGHTS_FIRST_DIGIT);
        int secondDigit = calculateDigit(text.substring(0, 9) + firstDigit, WEIGHTS_SECOND_DIGIT);

        return Character.getNumericValue(text.charAt(9)) == firstDigit
                && Character.getNumericValue(text.charAt(10)) == secondDigit;
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
        Cpf cpf = (Cpf) o;
        return Objects.equals(value, cpf.value);
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
