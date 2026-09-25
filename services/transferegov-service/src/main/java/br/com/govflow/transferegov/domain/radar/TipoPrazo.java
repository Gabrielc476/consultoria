package br.com.govflow.transferegov.domain.radar;

/**
 * Tipos de marcos temporais monitorados para convênios do SICONV/Transferegov.
 */
public enum TipoPrazo {
    CLAUSULA_SUSPENSIVA("Cláusula Suspensiva", "Prazo limite para cumprimento de condicionantes da Fase 2"),
    FIM_VIGENCIA("Fim da Vigência", "Data final de vigência do instrumento de repasse federal"),
    PRESTACAO_CONTAS("Prestação de Contas", "Prazo limite legal para envio da prestação de contas final");

    private final String descricao;
    private final String detalhe;

    TipoPrazo(String descricao, String detalhe) {
        this.descricao = descricao;
        this.detalhe = detalhe;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getDetalhe() {
        return detalhe;
    }
}
