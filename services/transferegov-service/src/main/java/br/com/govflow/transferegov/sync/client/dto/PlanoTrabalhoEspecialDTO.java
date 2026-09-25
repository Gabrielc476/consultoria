package br.com.govflow.transferegov.sync.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PlanoTrabalhoEspecialDTO(
        @JsonProperty("id_plano_trabalho") Long idPlanoTrabalho,
        @JsonProperty("dt_hora_situacao_plano_trabalho") String dtHoraSituacaoPlanoTrabalho,
        @JsonProperty("situacao_plano_trabalho") String situacaoPlanoTrabalho,
        @JsonProperty("ind_orcamento_proprio_plano_trabalho") String indOrcamentoProprioPlanoTrabalho,
        @JsonProperty("data_inicio_execucao_plano_trabalho") LocalDate dataInicioExecucaoPlanoTrabalho,
        @JsonProperty("data_fim_execucao_plano_trabalho") LocalDate dataFimExecucaoPlanoTrabalho,
        @JsonProperty("prazo_execucao_meses_plano_trabalho") Integer prazoExecucaoMesesPlanoTrabalho,
        @JsonProperty("id_plano_acao") Long idPlanoAcao,
        @JsonProperty("classificacao_orcamentaria_pt") String classificacaoOrcamentariaPt,
        @JsonProperty("ind_justificativa_prorrogacao_atraso_pt") String indJustificativaProrrogacaoAtrasoPt,
        @JsonProperty("ind_justificativa_prorrogacao_paralizacao_pt") String indJustificativaProrrogacaoParalizacaoPt,
        @JsonProperty("justificativa_prorrogacao_paralizacao_pt") String justificativaProrrogacaoParalizacaoPt,
        @JsonProperty("dt_hora_pt_aprovado") String dtHoraPtAprovado,
        @JsonProperty("ind_orgao_analises_pendentes") String indOrgaoAnalisesPendentes
) {
    public boolean isAprovado() {
        return "APROVADO".equalsIgnoreCase(situacaoPlanoTrabalho)
                || "CONCLUIDO".equalsIgnoreCase(situacaoPlanoTrabalho);
    }
}
