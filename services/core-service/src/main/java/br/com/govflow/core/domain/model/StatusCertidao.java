package br.com.govflow.core.domain.model;

/**
 * Semáforo de criticidade e conformidade de cada certidão do CAUC:
 * - REGULAR: Mais de 10 dias para o vencimento.
 * - ALERTA: Faltando 10 ou 5 dias (janela de risco para renovação).
 * - VENCIDA: Expirada ou com restrição ativa impeditiva de repasses.
 */
public enum StatusCertidao {
    REGULAR,
    ALERTA,
    VENCIDA;

    public static StatusCertidao fromSituacaoBanco(String situacao) {
        if (situacao == null || situacao.isBlank()) {
            return REGULAR;
        }
        return switch (situacao.trim().toUpperCase()) {
            case "REGULAR" -> REGULAR;
            case "ALERTA", "EM_RISCO" -> ALERTA;
            case "VENCIDA", "IRREGULAR", "INADIMPLENTE" -> VENCIDA;
            default -> REGULAR;
        };
    }
}
