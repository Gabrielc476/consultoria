package br.com.govflow.transferegov.domain.compliance;

import java.util.Collections;
import java.util.List;

public record Adpf854ComplianceResult(
        StatusAdpf854 status,
        boolean possuiPlanoTrabalho,
        boolean planoTrabalhoAprovado,
        boolean possuiRelatorioGestao,
        boolean relatorioGestaoDisponibilizado,
        boolean dadosBancariosValidos,
        List<InconformidadeItem> inconformidades
) {
    public Adpf854ComplianceResult {
        if (inconformidades == null) {
            inconformidades = Collections.emptyList();
        }
    }

    public record InconformidadeItem(
            TipoInconformidadeAdpf854 tipo,
            SeveridadeInconformidade severidade,
            String descricao
    ) {}
}
