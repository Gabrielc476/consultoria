package br.com.govflow.core.infrastructure.adapter.in.rest.dto.request;

import br.com.govflow.core.domain.model.PorteMunicipio;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CadastrarPrefeituraRequest(
        @NotBlank(message = "CNPJ é obrigatório.")
        String cnpj,

        @NotBlank(message = "Razão Social é obrigatória.")
        @Size(max = 200, message = "Razão Social não pode ultrapassar 200 caracteres.")
        String razaoSocial,

        @NotBlank(message = "Nome do Município é obrigatório.")
        @Size(max = 100, message = "Nome do Município não pode ultrapassar 100 caracteres.")
        String nomeMunicipio,

        @NotBlank(message = "UF é obrigatória.")
        @Size(min = 2, max = 2, message = "UF deve conter exatamente 2 letras.")
        String uf,

        @NotBlank(message = "Código IBGE é obrigatório.")
        String codigoIbge,

        @NotNull(message = "Porte do Município é obrigatório.")
        PorteMunicipio porteMunicipio,

        String nomePrefeito,

        String cpfPrefeito,

        LocalDate inicioMandato,

        LocalDate fimMandato
) {}
