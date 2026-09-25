package br.com.govflow.transferegov.domain.compliance;

import br.com.govflow.transferegov.domain.compliance.Adpf854ComplianceResult.InconformidadeItem;
import br.com.govflow.transferegov.sync.client.dto.PlanoAcaoEspecialDTO;
import br.com.govflow.transferegov.sync.client.dto.PlanoTrabalhoEspecialDTO;
import br.com.govflow.transferegov.sync.client.dto.RelatorioGestaoEspecialDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Motor de Avaliação de Conformidade das Emendas Especiais (Pix) com o STF (ADPF 854)
 * e a Portaria Conjunta MGI/MF/CGU nº 33/2023.
 */
@Component
public class Adpf854ComplianceEvaluator {

    public Adpf854ComplianceResult avaliar(
            PlanoAcaoEspecialDTO planoAcao,
            List<PlanoTrabalhoEspecialDTO> planosTrabalho,
            List<RelatorioGestaoEspecialDTO> relatoriosGestao,
            LocalDate dataReferencia
    ) {
        if (dataReferencia == null) {
            dataReferencia = LocalDate.now();
        }

        List<InconformidadeItem> inconformidades = new ArrayList<>();

        // 1. Verificação de Impedimento do Plano de Ação
        if (planoAcao != null && "IMPEDIDO".equalsIgnoreCase(planoAcao.situacaoPlanoAcao())) {
            String motivo = planoAcao.motivoImpedimentoPlanoAcao() != null
                    ? planoAcao.motivoImpedimentoPlanoAcao()
                    : "Plano de ação marcado como IMPEDIDO no Transferegov";
            inconformidades.add(new InconformidadeItem(
                    TipoInconformidadeAdpf854.TRANSFERENCIA_IMPEDIDA,
                    SeveridadeInconformidade.CRITICO,
                    "Transferência Especial impedida: " + motivo
            ));
        }

        // 2. Verificação de Dados Bancários da Conta Vinculada Exclusiva
        boolean dadosBancariosValidos = true;
        if (planoAcao == null
                || isVazio(planoAcao.numeroContaPlanoAcao())
                || isVazio(planoAcao.numeroAgenciaPlanoAcao())
                || isVazio(planoAcao.nomeBancoPlanoAcao())) {
            dadosBancariosValidos = false;
            inconformidades.add(new InconformidadeItem(
                    TipoInconformidadeAdpf854.DADOS_BANCARIOS_AUSENTES,
                    SeveridadeInconformidade.ALERTA,
                    "Conta corrente vinculada exclusiva não cadastrada ou incompleta para a Emenda Pix"
            ));
        }

        // 3. Verificação de Existência e Aprovação do Plano de Trabalho (Transparência Ativa)
        boolean possuiPlanoTrabalho = planosTrabalho != null && !planosTrabalho.isEmpty();
        boolean planoTrabalhoAprovado = false;

        if (!possuiPlanoTrabalho) {
            inconformidades.add(new InconformidadeItem(
                    TipoInconformidadeAdpf854.AUSENCIA_PLANO_TRABALHO,
                    SeveridadeInconformidade.CRITICO,
                    "Ausência de Plano de Trabalho cadastrado no Transferegov para aplicação dos recursos (STF ADPF 854)"
            ));
        } else {
            planoTrabalhoAprovado = planosTrabalho.stream().anyMatch(PlanoTrabalhoEspecialDTO::isAprovado);
            if (!planoTrabalhoAprovado) {
                boolean algumReprovado = planosTrabalho.stream()
                        .anyMatch(pt -> "REPROVADO".equalsIgnoreCase(pt.situacaoPlanoTrabalho()));

                if (algumReprovado) {
                    inconformidades.add(new InconformidadeItem(
                            TipoInconformidadeAdpf854.PLANO_TRABALHO_NAO_APROVADO,
                            SeveridadeInconformidade.CRITICO,
                            "Plano de Trabalho foi formalmente REPROVADO no Transferegov"
                    ));
                } else {
                    inconformidades.add(new InconformidadeItem(
                            TipoInconformidadeAdpf854.PLANO_TRABALHO_NAO_APROVADO,
                            SeveridadeInconformidade.ALERTA,
                            "Plano de Trabalho pendente de aprovação (status atual: "
                                    + planosTrabalho.get(0).situacaoPlanoTrabalho() + ")"
                    ));
                }
            }
        }

        // 4. Verificação de Relatórios de Gestão (Prestação de Contas e Prazos)
        boolean possuiRelatorioGestao = relatoriosGestao != null && !relatoriosGestao.isEmpty();
        boolean relatorioDisponibilizado = false;

        LocalDate maxFimExecucao = null;
        if (possuiPlanoTrabalho) {
            for (PlanoTrabalhoEspecialDTO pt : planosTrabalho) {
                if (pt.dataFimExecucaoPlanoTrabalho() != null) {
                    if (maxFimExecucao == null || pt.dataFimExecucaoPlanoTrabalho().isAfter(maxFimExecucao)) {
                        maxFimExecucao = pt.dataFimExecucaoPlanoTrabalho();
                    }
                }
            }
        }

        boolean planoExecutadoOuExpirado = maxFimExecucao != null && maxFimExecucao.isBefore(dataReferencia);

        if (planoExecutadoOuExpirado) {
            if (!possuiRelatorioGestao) {
                inconformidades.add(new InconformidadeItem(
                        TipoInconformidadeAdpf854.AUSENCIA_RELATORIO_GESTAO,
                        SeveridadeInconformidade.CRITICO,
                        "Prazo de execução finalizado em " + maxFimExecucao + " sem nenhum Relatório de Gestão cadastrado"
                ));
            } else {
                relatorioDisponibilizado = relatoriosGestao.stream().anyMatch(RelatorioGestaoEspecialDTO::isDisponibilizado);
                if (!relatorioDisponibilizado) {
                    inconformidades.add(new InconformidadeItem(
                            TipoInconformidadeAdpf854.PRAZO_RELATORIO_EXPIRADO,
                            SeveridadeInconformidade.CRITICO,
                            "Prazo de execução expirado e o Relatório de Gestão ainda permanece em elaboração sem disponibilização"
                    ));
                }
            }
        } else if (possuiRelatorioGestao) {
            relatorioDisponibilizado = relatoriosGestao.stream().anyMatch(RelatorioGestaoEspecialDTO::isDisponibilizado);
            if (!relatorioDisponibilizado) {
                inconformidades.add(new InconformidadeItem(
                        TipoInconformidadeAdpf854.RELATORIO_GESTAO_NAO_DISPONIBILIZADO,
                        SeveridadeInconformidade.ALERTA,
                        "Relatório de Gestão em elaboração aguardando disponibilização final"
                ));
            }

            for (RelatorioGestaoEspecialDTO rg : relatoriosGestao) {
                if (rg.valorPendenteRelatorioGestaoNovo() != null
                        && rg.valorPendenteRelatorioGestaoNovo().compareTo(BigDecimal.ZERO) > 0) {
                    inconformidades.add(new InconformidadeItem(
                            TipoInconformidadeAdpf854.VALOR_PENDENTE_SEM_COMPROVACAO,
                            SeveridadeInconformidade.ALERTA,
                            "Relatório de gestão possui saldo financeiro pendente de comprovação: R$ "
                                    + rg.valorPendenteRelatorioGestaoNovo()
                    ));
                }
            }
        }

        // 5. Consolidação do Status
        StatusAdpf854 statusFinal;
        boolean temCritico = inconformidades.stream().anyMatch(i -> i.severidade() == SeveridadeInconformidade.CRITICO);
        boolean temAlerta = inconformidades.stream().anyMatch(i -> i.severidade() == SeveridadeInconformidade.ALERTA);

        if (temCritico) {
            statusFinal = StatusAdpf854.NAO_CONFORME;
        } else if (temAlerta) {
            statusFinal = StatusAdpf854.ALERTA;
        } else {
            statusFinal = StatusAdpf854.CONFORME;
        }

        return new Adpf854ComplianceResult(
                statusFinal,
                possuiPlanoTrabalho,
                planoTrabalhoAprovado,
                possuiRelatorioGestao,
                relatorioDisponibilizado,
                dadosBancariosValidos,
                inconformidades
        );
    }

    private boolean isVazio(String s) {
        return s == null || s.trim().isEmpty();
    }
}
