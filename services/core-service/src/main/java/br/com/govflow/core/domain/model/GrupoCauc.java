package br.com.govflow.core.domain.model;

/**
 * Representa os 4 grupos legais de exigências fiscais e orçamentárias
 * estabelecidos pela Instrução Normativa STN nº 01/2021 e art. 25 da LRF.
 */
public enum GrupoCauc {
    TRIBUTOS_FGTS("Grupo I: Obrigações Financeiras"),
    PRESTACAO_CONTAS("Grupo II: Adimplência Financeira"),
    SICONFI_FISCAL("Grupo III: Prestação de Contas e Transparência Fiscal"),
    LIMITES_CONSTITUCIONAIS("Grupo IV: Limites Constitucionais e Legais");

    private final String descricao;

    GrupoCauc(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
