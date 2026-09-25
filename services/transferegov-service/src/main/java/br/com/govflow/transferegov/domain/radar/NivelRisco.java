package br.com.govflow.transferegov.domain.radar;

/**
 * Níveis de risco para o semáforo de criticidade de prazos do Transferegov.
 * - CRITICO (Vermelho): Prazo vencido (< 0 dias) ou restando até 15 dias corridos.
 * - ATENCAO (Amarelo): Entre 16 e 60 dias restantes.
 * - REGULAR (Verde): Mais de 60 dias restantes.
 */
public enum NivelRisco {
    CRITICO(1, "Crítico", "Menos de 15 dias restantes ou já vencido"),
    ATENCAO(2, "Atenção", "Entre 16 e 60 dias restantes"),
    REGULAR(3, "Regular", "Mais de 60 dias restantes");

    private final int prioridade;
    private final String label;
    private final String descricao;

    NivelRisco(int prioridade, String label, String descricao) {
        this.prioridade = prioridade;
        this.label = label;
        this.descricao = descricao;
    }

    public int getPrioridade() {
        return prioridade;
    }

    public String getLabel() {
        return label;
    }

    public String getDescricao() {
        return descricao;
    }

    public static NivelRisco fromDiasRestantes(long dias) {
        if (dias <= 15) {
            return CRITICO;
        } else if (dias <= 60) {
            return ATENCAO;
        } else {
            return REGULAR;
        }
    }
}
