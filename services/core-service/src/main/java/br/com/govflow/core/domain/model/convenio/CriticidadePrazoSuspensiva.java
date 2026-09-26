package br.com.govflow.core.domain.model.convenio;

/**
 * Níveis de criticidade do cronômetro fatal de 180 dias da Cláusula Suspensiva.
 */
public enum CriticidadePrazoSuspensiva {

    /**
     * Mais de 90 dias restantes: situação confortável para elaboração e análise.
     */
    REGULAR("Regular"),

    /**
     * Entre 31 e 90 dias restantes: atenção redobrada com tramitação na Caixa.
     */
    ATENCAO("Atenção"),

    /**
     * 30 dias ou menos restantes: risco iminente de perda da verba; exige plantão técnico e pedido de prorrogação.
     */
    CRITICO("Crítico"),

    /**
     * Prazo vencido sem superação nem pedido de prorrogação acolhido: risco de rescisão sumária.
     */
    EXPIRADO("Expirado");

    private final String descricao;

    CriticidadePrazoSuspensiva(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
