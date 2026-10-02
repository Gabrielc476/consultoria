package br.com.govflow.core.domain.model.documento;

import java.util.Arrays;
import java.util.Optional;

/**
 * As 10 Fases Oficiais do Ciclo de Vida do Convênio / Contrato de Repasse no GovFlow.
 * Cada fase corresponde a um diretório físico no Ficheiro Digital.
 */
public enum FaseCicloVida {
    FASE_00_PROPOSTA(0, "Fase 00", "00_Habilitacao_e_Proposta", "Habilitação e Proposta"),
    FASE_01_CELEBRACAO(1, "Fase 01", "01_Celebracao_e_Formalizacao", "Celebração e Formalização"),
    FASE_02_CLAUSULA_SUSPENSIVA(2, "Fase 02", "02_Clausula_Suspensiva_e_Engenharia", "Cláusula Suspensiva e Engenharia"),
    FASE_03_LICITACAO(3, "Fase 03", "03_Licitacao_e_Contratacao", "Licitação e Contratação"),
    FASE_04_EXECUCAO_FISICA(4, "Fase 04", "04_Execucao_Fisica_e_Medicoes", "Execução Física e Medições"),
    FASE_05_EXECUCAO_FINANCEIRA(5, "Fase 05", "05_Execucao_Financeira_e_Pagamentos", "Execução Financeira e Pagamentos"),
    FASE_06_ALTERACOES_CONTRATUAIS(6, "Fase 06", "06_Alteracoes_Contratuais_e_Aditivos", "Alterações Contratuais e Aditivos"),
    FASE_07_PRESTACAO_CONTAS(7, "Fase 07", "07_Prestacao_Contas_e_Termo_Recebimento", "Prestação de Contas e Termo de Recebimento"),
    FASE_08_ENCERRAMENTO_FINANCEIRO(8, "Fase 08", "08_Encerramento_Financeiro_e_Saldos", "Encerramento Financeiro e Saldos"),
    FASE_09_PASSIVO_JURIDICO(9, "Fase 09", "09_Notificacoes_e_Passivo_Juridico", "Notificações e Passivo Jurídico");

    private final int numero;
    private final String codigo;
    private final String nomePasta;
    private final String descricao;

    FaseCicloVida(int numero, String codigo, String nomePasta, String descricao) {
        this.numero = numero;
        this.codigo = codigo;
        this.nomePasta = nomePasta;
        this.descricao = descricao;
    }

    public int getNumero() {
        return numero;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNomePasta() {
        return nomePasta;
    }

    public String getDescricao() {
        return descricao;
    }

    public static Optional<FaseCicloVida> porNumero(int numero) {
        return Arrays.stream(values())
                .filter(f -> f.numero == numero)
                .findFirst();
    }

    public static FaseCicloVida fromCodigoOuNome(String valor) {
        if (valor == null || valor.isBlank()) {
            return FASE_05_EXECUCAO_FINANCEIRA;
        }
        String normalizado = valor.trim().toUpperCase();

        for (FaseCicloVida f : values()) {
            if (f.name().equalsIgnoreCase(normalizado) ||
                f.codigo.equalsIgnoreCase(normalizado) ||
                f.nomePasta.equalsIgnoreCase(normalizado) ||
                String.valueOf(f.numero).equals(normalizado) ||
                ("0" + f.numero).equals(normalizado)) {
                return f;
            }
        }

        // Tenta mapear nomes parciais comuns
        if (normalizado.contains("PROPOSTA") || normalizado.contains("HABILITACAO")) return FASE_00_PROPOSTA;
        if (normalizado.contains("CELEBRACAO") || normalizado.contains("FORMALIZACAO")) return FASE_01_CELEBRACAO;
        if (normalizado.contains("SUSPENSIVA") || normalizado.contains("ENGENHARIA")) return FASE_02_CLAUSULA_SUSPENSIVA;
        if (normalizado.contains("LICITACAO") || normalizado.contains("CONTRATACAO")) return FASE_03_LICITACAO;
        if (normalizado.contains("MEDICAO") || normalizado.contains("FISICA")) return FASE_04_EXECUCAO_FISICA;
        if (normalizado.contains("FINANCEIRA") || normalizado.contains("PAGAMENTO") || normalizado.contains("OBTV")) return FASE_05_EXECUCAO_FINANCEIRA;
        if (normalizado.contains("ADITIVO") || normalizado.contains("ALTERACAO")) return FASE_06_ALTERACOES_CONTRATUAIS;
        if (normalizado.contains("PRESTACAO") || normalizado.contains("RECEBIMENTO")) return FASE_07_PRESTACAO_CONTAS;
        if (normalizado.contains("ENCERRAMENTO") || normalizado.contains("SALDO") || normalizado.contains("GRU")) return FASE_08_ENCERRAMENTO_FINANCEIRO;
        if (normalizado.contains("NOTIFICACAO") || normalizado.contains("PASSIVO") || normalizado.contains("JURIDICO") || normalizado.contains("TCE")) return FASE_09_PASSIVO_JURIDICO;

        return FASE_05_EXECUCAO_FINANCEIRA;
    }
}
