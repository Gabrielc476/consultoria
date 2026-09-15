package br.com.govflow.core.domain.model;

public enum PorteMunicipio {
    PEQUENO_PORTE_1("Até 20.000 habitantes"),
    PEQUENO_PORTE_2("De 20.001 a 50.000 habitantes"),
    MEDIO_PORTE("De 50.001 a 100.000 habitantes"),
    GRANDE_PORTE("Acima de 100.000 habitantes");

    private final String descricao;

    PorteMunicipio(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
