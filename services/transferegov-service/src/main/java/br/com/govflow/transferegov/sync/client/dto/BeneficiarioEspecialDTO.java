package br.com.govflow.transferegov.sync.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BeneficiarioEspecialDTO(
        @JsonProperty("id_beneficiario") Long idBeneficiario,
        @JsonProperty("uf_beneficiario") String ufBeneficiario,
        @JsonProperty("nome_beneficiario") String nomeBeneficiario,
        @JsonProperty("cnpj_beneficiario") String cnpjBeneficiario,
        @JsonProperty("id_ente") Long idEnte
) {}
