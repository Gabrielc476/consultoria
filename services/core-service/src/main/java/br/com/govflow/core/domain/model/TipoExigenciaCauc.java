package br.com.govflow.core.domain.model;

import java.util.Arrays;
import java.util.Optional;

/**
 * Catálogo exaustivo das 16 exigências fiscais e orçamentárias monitoradas pelo CAUC
 * (Instrução Normativa STN nº 01/2021 e art. 25 da Lei de Responsabilidade Fiscal).
 */
public enum TipoExigenciaCauc {

    // =========================================================================
    // GRUPO I: Obrigações Financeiras e Débitos Trabalhistas
    // =========================================================================
    RECEITA_FEDERAL_PGFN(
            "1.1",
            GrupoCauc.TRIBUTOS_FGTS,
            "Certidão Negativa de Débitos Relativos a Créditos Tributários Federais e à Dívida Ativa da União",
            "RFB / PGFN"
    ),
    REGULARIDADE_FGTS(
            "1.2",
            GrupoCauc.TRIBUTOS_FGTS,
            "Certificado de Regularidade do FGTS (CRF)",
            "Caixa Econômica Federal"
    ),
    REGULARIDADE_PREVIDENCIARIA(
            "1.3",
            GrupoCauc.TRIBUTOS_FGTS,
            "Certificado de Regularidade Previdenciária (CRP / RPPS ou Regime Geral)",
            "Ministério da Previdência Social"
    ),
    DEBITOS_TRABALHISTAS_CNDT(
            "1.4",
            GrupoCauc.TRIBUTOS_FGTS,
            "Certidão Negativa de Débitos Trabalhistas (CNDT)",
            "Tribunal Superior do Trabalho"
    ),

    // =========================================================================
    // GRUPO II: Adimplência Financeira, Prestação de Contas e CADIN
    // =========================================================================
    PRESTACAO_CONTAS_RECURSOS_FEDERAIS(
            "2.1",
            GrupoCauc.PRESTACAO_CONTAS,
            "Prestação de Contas de Recursos Federais Recebidos (SIAFI / Transferegov)",
            "STN / Órgãos Concedentes"
    ),
    CADIN_FEDERAL(
            "2.2",
            GrupoCauc.PRESTACAO_CONTAS,
            "CADIN Federal - Registro Informativo de Créditos não Quitados do Setor Público",
            "SISBACEN / STN"
    ),
    PRESTACAO_CONTAS_FNDE_FNS(
            "2.3",
            GrupoCauc.PRESTACAO_CONTAS,
            "Prestação de Contas de Recursos Repassados Fundo a Fundo (FNDE / FNS)",
            "FNDE / FNS"
    ),
    PRECATORIOS_JUDICIAIS(
            "2.4",
            GrupoCauc.PRESTACAO_CONTAS,
            "Regularidade no Pagamento de Precatórios Judiciais",
            "Tribunal de Justiça / TRF"
    ),

    // =========================================================================
    // GRUPO III: Prestação de Contas, Transparência Fiscal e Balanços (SICONFI)
    // =========================================================================
    RREO_SICONFI(
            "3.1",
            GrupoCauc.SICONFI_FISCAL,
            "Relatório Resumido da Execução Orçamentária (RREO)",
            "STN / SICONFI"
    ),
    RGF_SICONFI(
            "3.2",
            GrupoCauc.SICONFI_FISCAL,
            "Relatório de Gestão Fiscal (RGF)",
            "STN / SICONFI"
    ),
    BALANCO_ANUAL_SICONFI(
            "3.3",
            GrupoCauc.SICONFI_FISCAL,
            "Homologação do Balanço Anual e Matriz de Saldos Contábeis (DCA)",
            "STN / SICONFI"
    ),
    CONTAS_ANUAIS_TCE(
            "3.4",
            GrupoCauc.SICONFI_FISCAL,
            "Encaminhamento das Contas Anuais ao Tribunal de Contas do Estado",
            "TCE / SICONFI"
    ),

    // =========================================================================
    // GRUPO IV: Limites Constitucionais e Legais
    // =========================================================================
    APLICACAO_SAUDE_SIOPS(
            "4.1",
            GrupoCauc.LIMITES_CONSTITUCIONAIS,
            "Aplicação Mínima em Ações e Serviços Públicos de Saúde (15%)",
            "SIOPS / Ministério da Saúde"
    ),
    APLICACAO_EDUCACAO_SIOPE(
            "4.2",
            GrupoCauc.LIMITES_CONSTITUCIONAIS,
            "Aplicação Mínima em Desenvolvimento e Manutenção do Ensino (25%)",
            "SIOPE / FNDE"
    ),
    DESPESA_PESSOAL_LRF(
            "4.3",
            GrupoCauc.LIMITES_CONSTITUCIONAIS,
            "Limite com Despesa Total de Pessoal (54% Poder Executivo - LRF)",
            "STN / SICONFI"
    ),
    DIVIDA_CONSOLIDADA_CREDITO(
            "4.4",
            GrupoCauc.LIMITES_CONSTITUCIONAIS,
            "Dívida Consolidada Líquida e Operações de Crédito / ARO",
            "STN / COMIEX"
    );

    private final String codigo;
    private final GrupoCauc grupo;
    private final String nome;
    private final String orgaoEmissor;

    TipoExigenciaCauc(String codigo, GrupoCauc grupo, String nome, String orgaoEmissor) {
        this.codigo = codigo;
        this.grupo = grupo;
        this.nome = nome;
        this.orgaoEmissor = orgaoEmissor;
    }

    public String getCodigo() {
        return codigo;
    }

    public GrupoCauc getGrupo() {
        return grupo;
    }

    public String getNome() {
        return nome;
    }

    public String getOrgaoEmissor() {
        return orgaoEmissor;
    }

    public static Optional<TipoExigenciaCauc> fromCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(item -> item.codigo.equalsIgnoreCase(codigo.trim()))
                .findFirst();
    }
}
