package br.com.govflow.core.domain.model.convenio;

/**
 * Estados do ciclo de vida de análise técnica de cada condicionante suspensiva junto à Mandatária Caixa.
 */
public enum StatusCondicionanteSuspensiva {

    /**
     * Município ainda está elaborando ou reunindo a documentação do pilar.
     */
    PENDENTE("Pendente de Submissão"),

    /**
     * Documentação protocolada no Transferegov e sob auditoria técnica dos engenheiros da Caixa GIGOV.
     */
    EM_ANALISE_CAIXA("Em Análise pela Caixa"),

    /**
     * Caixa emitiu Laudo de Pendências exigindo saneamento de pranchas, planilhas ou certidões dentro do prazo.
     */
    DILIGENCIA_EMITIDA("Diligência Emitida"),

    /**
     * Pilar aprovado tecnicamente pela Caixa com laudo favorável ou certidão validada.
     */
    APROVADO("Aprovado");

    private final String descricao;

    StatusCondicionanteSuspensiva(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
