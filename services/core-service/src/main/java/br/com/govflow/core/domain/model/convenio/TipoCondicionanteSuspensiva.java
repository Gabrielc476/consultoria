package br.com.govflow.core.domain.model.convenio;

/**
 * Os três pilares técnicos inegociáveis exigidos pela Caixa Econômica Federal (GIGOV / MN AE099)
 * para a eficácia e superação da Cláusula Suspensiva (Fase 2).
 */
public enum TipoCondicionanteSuspensiva {

    /**
     * Pilar 1: Projetos básicos e executivos de engenharia, memorial descritivo,
     * planilha balizada em SINAPI/SICRO, Curva ABC, BDI analítico (Acórdão TCU 2622/2013)
     * e ART/RRT quitada, culminando na emissão da SPA e LAE da Caixa.
     */
    ENGENHARIA_PROJETOS_SINAPI("Engenharia, Projetos & Orçamento SINAPI"),

    /**
     * Pilar 2: Licença Prévia e de Instalação (LP/LI), Licença Ambiental Simplificada/Única
     * ou Declaração Oficial de Inexigibilidade/Dispensa de Licenciamento Ambiental.
     */
    LICENCIAMENTO_AMBIENTAL("Licenciamento Ambiental"),

    /**
     * Pilar 3: Certidão de Inteiro Teor do Cartório de Registro de Imóveis (CRI)
     * atualizada (< 30-90 dias) ou Auto de Imissão Provisória na Posse transitada em julgado.
     */
    TITULARIDADE_IMOVEL("Comprovação de Titularidade do Imóvel");

    private final String descricao;

    TipoCondicionanteSuspensiva(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
