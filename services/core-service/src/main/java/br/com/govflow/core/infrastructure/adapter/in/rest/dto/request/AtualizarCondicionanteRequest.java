package br.com.govflow.core.infrastructure.adapter.in.rest.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AtualizarCondicionanteRequest(
        String numeroDocumentoComprobatorio,
        LocalDate dataValidade,
        String orgaoEmissor,
        BigDecimal valorOrcamentoAprovado,
        BigDecimal percentualBdiAprovado,
        String numeroArtRrt,
        String observacoes
) {
}
