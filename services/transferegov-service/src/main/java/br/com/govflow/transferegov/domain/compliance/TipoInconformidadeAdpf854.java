package br.com.govflow.transferegov.domain.compliance;

public enum TipoInconformidadeAdpf854 {
    AUSENCIA_PLANO_TRABALHO("Ausência de Plano de Trabalho cadastrado no Transferegov", SeveridadeInconformidade.CRITICO),
    PLANO_TRABALHO_NAO_APROVADO("Plano de Trabalho não aprovado pelo órgão competente", SeveridadeInconformidade.ALERTA),
    AUSENCIA_RELATORIO_GESTAO("Ausência de Relatório de Gestão para prestação de contas", SeveridadeInconformidade.CRITICO),
    RELATORIO_GESTAO_NAO_DISPONIBILIZADO("Relatório de Gestão permanece em elaboração sem disponibilização tempestiva", SeveridadeInconformidade.ALERTA),
    PRAZO_RELATORIO_EXPIRADO("Prazo legal de entrega do Relatório de Gestão expirado", SeveridadeInconformidade.CRITICO),
    DADOS_BANCARIOS_AUSENTES("Dados da conta corrente vinculada ausentes ou incompletos", SeveridadeInconformidade.ALERTA),
    TRANSFERENCIA_IMPEDIDA("Transferência especial com situação de impedimento declarada", SeveridadeInconformidade.CRITICO),
    VALOR_PENDENTE_SEM_COMPROVACAO("Relatório de Gestão possui valor pendente de execução financeira", SeveridadeInconformidade.ALERTA);

    private final String descricaoPadrao;
    private final SeveridadeInconformidade severidadePadrao;

    TipoInconformidadeAdpf854(String descricaoPadrao, SeveridadeInconformidade severidadePadrao) {
        this.descricaoPadrao = descricaoPadrao;
        this.severidadePadrao = severidadePadrao;
    }

    public String getDescricaoPadrao() {
        return descricaoPadrao;
    }

    public SeveridadeInconformidade getSeveridadePadrao() {
        return severidadePadrao;
    }
}
