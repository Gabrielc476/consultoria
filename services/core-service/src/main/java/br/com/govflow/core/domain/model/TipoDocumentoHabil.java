package br.com.govflow.core.domain.model;

public enum TipoDocumentoHabil {
    NOTA_FISCAL_SERVICOS,
    NOTA_FISCAL_MERCADORIAS,
    RECIBO_LEGAL,
    BOLETIM_MEDICAO,
    MEDICAO_OBRAS,
    // Novos tipos admitidos para auditoria polimórfica e agnóstica
    PROJETO_ENGENHARIA,
    LICENCA_AMBIENTAL,
    LICITACAO,
    CONTRATO_ADMINISTRATIVO,
    TERMO_CONVENIO,
    CERTIDAO_CAUC,
    TERMO_ADITIVO,
    TERMO_RECEBIMENTO,
    RELATORIO_CUMPRIMENTO_OBJETO,
    NOTIFICACAO_PASSIVO,
    OUTROS_DOCUMENTOS,
    DOCUMENTO_GENERICO;

    public boolean isFiscal() {
        return this == NOTA_FISCAL_SERVICOS || this == NOTA_FISCAL_MERCADORIAS || this == RECIBO_LEGAL;
    }

    public boolean isMedicao() {
        return this == BOLETIM_MEDICAO || this == MEDICAO_OBRAS;
    }
}
