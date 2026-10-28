package br.com.govflow.core.infrastructure.adapter.in.rest.dto.request;

import br.com.govflow.core.domain.model.licitacao.CriterioJulgamento;
import br.com.govflow.core.domain.model.licitacao.ModalidadeLicitacao;
import br.com.govflow.core.domain.model.licitacao.RegimeExecucao;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CadastrarLicitacaoRequest(
        @Schema(description = "Número do processo administrativo municipal", example = "001/2026")
        String numeroProcessoAdministrativo,

        @NotBlank(message = "O número da licitação é obrigatório.")
        @Schema(description = "Identificador do certame", example = "Concorrência 002/2026")
        String numeroLicitacao,

        @NotNull(message = "A modalidade é obrigatória.")
        @Schema(description = "Modalidade da Lei 14.133/2021")
        ModalidadeLicitacao modalidade,

        CriterioJulgamento criterioJulgamento,

        RegimeExecucao regimeExecucao,

        @NotBlank(message = "O objeto da licitação é obrigatório.")
        String objeto,

        @NotNull(message = "O valor estimado é obrigatório.")
        @DecimalMin(value = "0.01", message = "O valor estimado deve ser maior que zero.")
        BigDecimal valorEstimado,

        LocalDate dataPublicacaoEdital,

        LocalDate dataAberturaPropostas,

        String linkPncp,

        String linkTransferegov,

        String linkSistemaCompras
) {}
