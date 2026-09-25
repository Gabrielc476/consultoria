package br.com.govflow.transferegov.sync.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PlanoAcaoEspecialDTO(
        @JsonProperty("id_plano_acao") Long idPlanoAcao,
        @JsonProperty("codigo_plano_acao") String codigoPlanoAcao,
        @JsonProperty("ano_plano_acao") Integer anoPlanoAcao,
        @JsonProperty("modalidade_plano_acao") String modalidadePlanoAcao,
        @JsonProperty("situacao_plano_acao") String situacaoPlanoAcao,
        @JsonProperty("email_camara") String emailCamara,
        @JsonProperty("data_aceite_plano_acao") LocalDate dataAceitePlanoAcao,
        @JsonProperty("codigo_banco_plano_acao") String codigoBancoPlanoAcao,
        @JsonProperty("codigo_situacao_dado_bancario_plano_acao") Integer codigoSituacaoDadoBancarioPlanoAcao,
        @JsonProperty("descricao_situacao_dado_bancario_plano_acao") String descricaoSituacaoDadoBancarioPlanoAcao,
        @JsonProperty("nome_banco_plano_acao") String nomeBancoPlanoAcao,
        @JsonProperty("numero_agencia_plano_acao") String numeroAgenciaPlanoAcao,
        @JsonProperty("dv_agencia_plano_acao") String dvAgenciaPlanoAcao,
        @JsonProperty("numero_conta_plano_acao") String numeroContaPlanoAcao,
        @JsonProperty("dv_conta_plano_acao") String dvContaPlanoAcao,
        @JsonProperty("nome_parlamentar_emenda_plano_acao") String nomeParlamentarEmendaPlanoAcao,
        @JsonProperty("ano_emenda_parlamentar_plano_acao") Integer anoEmendaParlamentarPlanoAcao,
        @JsonProperty("codigo_parlamentar_emenda_plano_acao") Integer codigoParlamentarEmendaPlanoAcao,
        @JsonProperty("sequencial_emenda_parlamentar_plano_acao") Integer sequencialEmendaParlamentarPlanoAcao,
        @JsonProperty("numero_emenda_parlamentar_plano_acao") Integer numeroEmendaParlamentarPlanoAcao,
        @JsonProperty("codigo_emenda_parlamentar_formatado_plano_acao") String codigoEmendaParlamentarFormatadoPlanoAcao,
        @JsonProperty("categoria_despesa_plano_acao") String categoriaDespesaPlanoAcao,
        @JsonProperty("codigo_descricao_areas_politicas_publicas_plano_acao") String areasPoliticasPublicasPlanoAcao,
        @JsonProperty("descricao_programacao_orcamentaria_plano_acao") String programacaoOrcamentariaPlanoAcao,
        @JsonProperty("motivo_impedimento_plano_acao") String motivoImpedimentoPlanoAcao,
        @JsonProperty("valor_custeio_plano_acao") BigDecimal valorCusteioPlanoAcao,
        @JsonProperty("valor_investimento_plano_acao") BigDecimal valorInvestimentoPlanoAcao,
        @JsonProperty("id_agencia_conta") String idAgenciaConta,
        @JsonProperty("id_beneficiario") Long idBeneficiario,
        @JsonProperty("id_objeto") Integer idObjeto,
        @JsonProperty("nome_objeto") String nomeObjeto,
        @JsonProperty("detalhamento_objeto") String detalhamentoObjeto,
        @JsonProperty("id_programa") Integer idPrograma
) {
    public BigDecimal valorTotal() {
        BigDecimal custeio = valorCusteioPlanoAcao != null ? valorCusteioPlanoAcao : BigDecimal.ZERO;
        BigDecimal investimento = valorInvestimentoPlanoAcao != null ? valorInvestimentoPlanoAcao : BigDecimal.ZERO;
        return custeio.add(investimento);
    }
}
