package br.com.govflow.core.domain.model;

import java.util.Arrays;
import java.util.Optional;

public enum Uf {
    RO("11"), AC("12"), AM("13"), RR("14"), PA("15"), AP("16"), TO("17"),
    MA("21"), PI("22"), CE("23"), RN("24"), PB("25"), PE("26"), AL("27"), SE("28"), BA("29"),
    MG("31"), ES("32"), RJ("33"), SP("35"),
    PR("41"), SC("42"), RS("43"),
    MS("50"), MT("51"), GO("52"), DF("53");

    private final String codigoIbgeUf;

    Uf(String codigoIbgeUf) {
        this.codigoIbgeUf = codigoIbgeUf;
    }

    public String getCodigoIbgeUf() {
        return codigoIbgeUf;
    }

    public static Optional<Uf> fromString(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(u -> u.name().equalsIgnoreCase(value.trim()))
                .findFirst();
    }
}
