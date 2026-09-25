package br.com.govflow.transferegov.sync.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RelatorioGestaoEspecialDTO(
        @JsonProperty("id_relatorio_gestao_novo") Long idRelatorioGestaoNovo,
        @JsonProperty("data_relatorio_gestao_novo") LocalDate dataRelatorioGestaoNovo,
        @JsonProperty("data_e_hora_relatorio_gestao_novo") String dataHoraRelatorioGestaoNovo,
        @JsonProperty("tipo_relatorio_gestao_novo") String tipoRelatorioGestaoNovo,
        @JsonProperty("valor_executado_relatorio_gestao_novo") BigDecimal valorExecutadoRelatorioGestaoNovo,
        @JsonProperty("valor_pendente_relatorio_gestao_novo") BigDecimal valorPendenteRelatorioGestaoNovo,
        @JsonProperty("situacao_relatorio_gestao_novo") String situacaoRelatorioGestaoNovo,
        @JsonProperty("id_plano_acao") Long idPlanoAcao,
        @JsonProperty("id_executor") Integer idExecutor
) {
    public boolean isDisponibilizado() {
        return "DISPONIBILIZADO".equalsIgnoreCase(situacaoRelatorioGestaoNovo);
    }

    public boolean isFinal() {
        return "Final".equalsIgnoreCase(tipoRelatorioGestaoNovo);
    }
}
