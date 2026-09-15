package br.com.govflow.core.infrastructure.adapter.in.rest.dto.request;

import br.com.govflow.core.domain.model.PorteMunicipio;
import br.com.govflow.core.domain.model.StatusCauc;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record AtualizarPrefeituraRequest(
        @NotBlank(message = "Razão Social é obrigatória.")
        @Size(max = 200, message = "Razão Social não pode ultrapassar 200 caracteres.")
        String razaoSocial,

        @NotBlank(message = "Nome do Município é obrigatório.")
        @Size(max = 100, message = "Nome do Município não pode ultrapassar 100 caracteres.")
        String nomeMunicipio,

        @NotNull(message = "Porte do Município é obrigatório.")
        PorteMunicipio porteMunicipio,

        String nomePrefeito,

        String cpfPrefeito,

        LocalDate inicioMandato,

        LocalDate fimMandato,

        StatusCauc statusCauc
) {}
