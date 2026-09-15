package br.com.govflow.core.domain.model;

public enum PlanoConsultoria {
    STARTER(5),
    PRO(15),
    ENTERPRISE(999);

    private final int limitePrefeituras;

    PlanoConsultoria(int limitePrefeituras) {
        this.limitePrefeituras = limitePrefeituras;
    }

    public int getLimitePrefeituras() {
        return limitePrefeituras;
    }
}
