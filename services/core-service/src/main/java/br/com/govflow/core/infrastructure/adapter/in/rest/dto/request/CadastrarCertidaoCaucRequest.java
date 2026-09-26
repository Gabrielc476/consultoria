package br.com.govflow.core.infrastructure.adapter.in.rest.dto.request;

import br.com.govflow.core.domain.model.StatusCertidao;
import br.com.govflow.core.domain.model.TipoExigenciaCauc;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CadastrarCertidaoCaucRequest(
        @NotNull(message = "Tipo de exigência é obrigatório")
        TipoExigenciaCauc tipoExigencia,

        String numeroCertidao,

        @NotNull(message = "Data de emissão é obrigatória")
        LocalDate dataEmissao,

        @NotNull(message = "Data de validade é obrigatória")
        LocalDate dataValidade,

        String s3KeyComprovante,

        StatusCertidao situacao
) {}
