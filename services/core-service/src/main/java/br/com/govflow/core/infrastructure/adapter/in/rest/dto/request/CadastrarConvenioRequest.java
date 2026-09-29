package br.com.govflow.core.infrastructure.adapter.in.rest.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CadastrarConvenioRequest(
        @NotNull(message = "Prefeitura é obrigatória.")
        @Schema(description = "Identificador da prefeitura convenente", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        UUID prefeituraId,

        @NotBlank(message = "Número SICONV é obrigatório.")
        @Pattern(regexp = "^\\d{6}/\\d{4}$", message = "Número SICONV deve seguir o padrão 999999/AAAA.")
        @Schema(description = "Número do convênio ou contrato de repasse no SICONV/Transferegov", example = "954120/2026")
        String numeroSiconv,

        @Schema(description = "Número do processo administrativo no órgão concedente", example = "00124/2026")
        String numeroProcesso,

        @NotBlank(message = "Órgão Concedente é obrigatório.")
        @Schema(description = "Ministério, Fundo ou Entidade Concedente", example = "Ministério das Cidades (MCID)")
        String orgaoConcedente,

        @NotBlank(message = "Objeto do convênio é obrigatório.")
        @Schema(description = "Descrição detalhada do objeto acordado", example = "Pavimentação em Paralelepípedo e Drenagem no Bairro Monte Castelo")
        String objeto,

        @NotNull(message = "Valor Global é obrigatório.")
        @DecimalMin(value = "0.01", message = "Valor Global deve ser maior que zero.")
        @Schema(description = "Valor total pactuado no instrumento", example = "1000000.00")
        BigDecimal valorGlobal,

        @NotNull(message = "Valor de Repasse é obrigatório.")
        @DecimalMin(value = "0.01", message = "Valor de Repasse deve ser maior que zero.")
        @Schema(description = "Valor transferido pela União / Concedente", example = "950000.00")
        BigDecimal valorRepasse,

        @NotNull(message = "Valor de Contrapartida é obrigatório.")
        @Schema(description = "Valor custeado pelo município convenente", example = "50000.00")
        BigDecimal valorContrapartida,

        @Schema(description = "Indica se o convênio foi celebrado sob regime de Cláusula Suspensiva", example = "true")
        boolean possuiClausulaSuspensiva,

        @Schema(description = "Prazo fatal da cláusula suspensiva (se nulo e ativa, assume hoje + 180 dias)", example = "2026-10-31")
        LocalDate prazoClausulaSuspensiva,

        @Schema(description = "Data de início da vigência", example = "2026-05-01")
        LocalDate dataInicioVigencia,

        @NotNull(message = "Data de fim de vigência é obrigatória.")
        @Schema(description = "Data limite da vigência pactuada", example = "2027-12-31")
        LocalDate dataFimVigencia
) {}
