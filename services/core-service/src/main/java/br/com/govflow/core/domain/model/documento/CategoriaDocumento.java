package br.com.govflow.core.domain.model.documento;

import java.util.Arrays;

/**
 * Categorias documentais oficiais admitidas no Ficheiro Digital do GovFlow.
 */
public enum CategoriaDocumento {
    // Fase 00 - Proposta
    CERTIDAO_CAUC("Certidão CAUC / Regularidade Fiscal", FaseCicloVida.FASE_00_PROPOSTA),
    PROPOSTA_PLANO_TRABALHO("Proposta Formal e Plano de Trabalho", FaseCicloVida.FASE_00_PROPOSTA),
    PARECER_TECNICO_EMENDA("Parecer Técnico de Emenda / SIOP", FaseCicloVida.FASE_00_PROPOSTA),
    DOSSIE_DECLARACOES("Dossiê de Declarações Governamentais", FaseCicloVida.FASE_00_PROPOSTA),

    // Fase 01 - Celebração
    TERMO_CONVENIO("Termo de Convênio / Contrato de Repasse", FaseCicloVida.FASE_01_CELEBRACAO),
    PUBLICACAO_DOU("Publicação no Diário Oficial da União", FaseCicloVida.FASE_01_CELEBRACAO),
    NOTIFICACAO_CONTA_VINCULADA("Notificação de Abertura de Conta Op 006", FaseCicloVida.FASE_01_CELEBRACAO),

    // Fase 02 - Cláusula Suspensiva e Engenharia
    PROJETO_ENGENHARIA("Projeto Básico / Executivo de Engenharia", FaseCicloVida.FASE_02_CLAUSULA_SUSPENSIVA),
    PLANILHA_ORCAMENTARIA("Planilha Orçamentária SINAPI / Curva ABC", FaseCicloVida.FASE_02_CLAUSULA_SUSPENSIVA),
    LICENCA_AMBIENTAL("Licença Ambiental (LP/LI/LO)", FaseCicloVida.FASE_02_CLAUSULA_SUSPENSIVA),
    TITULARIDADE_IMOVEL("Titularidade do Imóvel / Desapropriação", FaseCicloVida.FASE_02_CLAUSULA_SUSPENSIVA),
    SPA_LAE_CAIXA("Síntese do Projeto Aprovado (SPA/LAE) Caixa", FaseCicloVida.FASE_02_CLAUSULA_SUSPENSIVA),

    // Fase 03 - Licitação
    LICITACAO("Edital de Licitação e Parecer Jurídico", FaseCicloVida.FASE_03_LICITACAO),
    ATA_HOMOLOGACAO_CERTAME("Atas de Sessão e Homologação", FaseCicloVida.FASE_03_LICITACAO),
    CONTRATO_ADMINISTRATIVO("Contrato Administrativo de Execução", FaseCicloVida.FASE_03_LICITACAO),
    PARECER_VRPL("Parecer VRPL Mandatária", FaseCicloVida.FASE_03_LICITACAO),
    AUTORIZACAO_INICIO_OBJETO("Autorização de Início de Objeto (AIO)", FaseCicloVida.FASE_03_LICITACAO),

    // Fase 04 - Execução Física
    BOLETIM_MEDICAO("Boletim de Medição de Obras", FaseCicloVida.FASE_04_EXECUCAO_FISICA),
    RELATORIO_FOTOGRAFICO("Relatório Fotográfico Georreferenciado", FaseCicloVida.FASE_04_EXECUCAO_FISICA),
    DIARIO_OBRA("Diário de Obra", FaseCicloVida.FASE_04_EXECUCAO_FISICA),
    RAE_CAIXA("Relatório de Acompanhamento (RAE) Caixa", FaseCicloVida.FASE_04_EXECUCAO_FISICA),

    // Fase 05 - Execução Financeira
    DOCUMENTO_HABIL("Documento Hábil Fiscal (NF-e, NFS-e, Recibo)", FaseCicloVida.FASE_05_EXECUCAO_FINANCEIRA),
    RETENCAO_TRIBUTARIA("Guia de Retenções Tributárias (DARF, DAM, GPS)", FaseCicloVida.FASE_05_EXECUCAO_FINANCEIRA),
    ORDEM_BANCARIA_OBTV("Comprovante de Liquidação / OBTV", FaseCicloVida.FASE_05_EXECUCAO_FINANCEIRA),
    EXTRATO_BANCARIO("Extrato Bancário da Conta Op 006", FaseCicloVida.FASE_05_EXECUCAO_FINANCEIRA),

    // Fase 06 - Aditivos
    TERMO_ADITIVO("Termo Aditivo de Vigência / Valor", FaseCicloVida.FASE_06_ALTERACOES_CONTRATUAIS),
    APOSTILAMENTO("Apostilamento de Reajuste (INCC/IPCA)", FaseCicloVida.FASE_06_ALTERACOES_CONTRATUAIS),
    PARECER_REPROGRAMACAO("Parecer de Reprogramação da Mandatária", FaseCicloVida.FASE_06_ALTERACOES_CONTRATUAIS),

    // Fase 07 - Prestação de Contas
    TERMO_RECEBIMENTO("Termo de Recebimento Provisório / Definitivo", FaseCicloVida.FASE_07_PRESTACAO_CONTAS),
    RELATORIO_CUMPRIMENTO_OBJETO("Relatório de Cumprimento do Objeto (RCO)", FaseCicloVida.FASE_07_PRESTACAO_CONTAS),
    PLACA_INAUGURACAO("Registro Fotográfico da Placa de Inauguração", FaseCicloVida.FASE_07_PRESTACAO_CONTAS),

    // Fase 08 - Encerramento Financeiro
    GUIA_RECOLHIMENTO_UNIAO("GRU de Recolhimento de Saldos Remanescentes", FaseCicloVida.FASE_08_ENCERRAMENTO_FINANCEIRO),
    COMPROVANTE_SALDO_ZERO("Comprovante Bancário Saldo Zero (Op 006)", FaseCicloVida.FASE_08_ENCERRAMENTO_FINANCEIRO),

    // Fase 09 - Passivo Jurídico
    NOTIFICACAO_DILIGENCIA("Notificação de Glosa / Diligência", FaseCicloVida.FASE_09_PASSIVO_JURIDICO),
    NOTIFICACAO_SELIC_45_DIAS("Notificação SELIC 45 Dias", FaseCicloVida.FASE_09_PASSIVO_JURIDICO),
    DEFESA_RECURSO("Peça de Defesa / Ação Súmula 230 TCU", FaseCicloVida.FASE_09_PASSIVO_JURIDICO),
    TOMADA_CONTAS_ESPECIAL("Processo de Tomada de Contas Especial (TCE)", FaseCicloVida.FASE_09_PASSIVO_JURIDICO),

    // Categoria Genérica
    OUTROS("Outros Documentos do Convênio", FaseCicloVida.FASE_05_EXECUCAO_FINANCEIRA);

    private final String descricao;
    private final FaseCicloVida fasePadrao;

    CategoriaDocumento(String descricao, FaseCicloVida fasePadrao) {
        this.descricao = descricao;
        this.fasePadrao = fasePadrao;
    }

    public String getDescricao() {
        return descricao;
    }

    public FaseCicloVida getFasePadrao() {
        return fasePadrao;
    }

    public static CategoriaDocumento fromString(String valor) {
        if (valor == null || valor.isBlank()) {
            return OUTROS;
        }
        String normalizado = valor.trim().toUpperCase();
        for (CategoriaDocumento cat : values()) {
            if (cat.name().equalsIgnoreCase(normalizado)) {
                return cat;
            }
        }
        // Tentativa de aproximação por substring
        if (normalizado.contains("MEDICAO") || normalizado.contains("BOLETIM") || normalizado.contains("BM")) return BOLETIM_MEDICAO;
        if (normalizado.contains("NOTA") || normalizado.contains("FISCAL") || normalizado.contains("NF")) return DOCUMENTO_HABIL;
        if (normalizado.contains("LICENCA")) return LICENCA_AMBIENTAL;
        if (normalizado.contains("PROJETO") || normalizado.contains("ART") || normalizado.contains("RRT")) return PROJETO_ENGENHARIA;
        if (normalizado.contains("LICITACAO") || normalizado.contains("EDITAL")) return LICITACAO;
        if (normalizado.contains("ADITIVO")) return TERMO_ADITIVO;
        if (normalizado.contains("CAUC") || normalizado.contains("CERTIDAO")) return CERTIDAO_CAUC;
        if (normalizado.contains("OBTV") || normalizado.contains("PAGAMENTO")) return ORDEM_BANCARIA_OBTV;
        if (normalizado.contains("EXTRATO")) return EXTRATO_BANCARIO;
        if (normalizado.contains("RECEBIMENTO")) return TERMO_RECEBIMENTO;
        if (normalizado.contains("GRU") || normalizado.contains("DEVOLUCAO")) return GUIA_RECOLHIMENTO_UNIAO;

        return OUTROS;
    }
}
