package br.com.govflow.transferegov.domain.radar;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO que consolida a análise de risco e os prazos calculados para um convênio monitorado.
 */
public record AlertaConvenioDTO(
        UUID convenioId,
        String nrConvenio,
        String idProposta,
        String municipio,
        String uf,
        String cnpjProponente,
        String nomeProponente,
        String objeto,
        String situacaoConvenio,
        NivelRisco nivelRisco,
        TipoPrazo tipoPrazoMaisProximo,
        LocalDate prazoMaisProximo,
        Long diasRestantes,
        LocalDate dataFimVigencia,
        Long diasFimVigencia,
        LocalDate dataSuspensiva,
        Long diasSuspensiva,
        LocalDate dataLimitePrestacaoContas,
        Long diasPrestacaoContas,
        BigDecimal valorGlobal,
        BigDecimal valorRepasse
) {
}
