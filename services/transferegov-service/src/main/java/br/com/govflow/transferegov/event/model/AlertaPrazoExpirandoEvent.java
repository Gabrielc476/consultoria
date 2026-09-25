package br.com.govflow.transferegov.event.model;

import br.com.govflow.transferegov.domain.radar.NivelRisco;
import br.com.govflow.transferegov.domain.radar.TipoPrazo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Evento disparado pelo motor de alertas quando um convênio atinge proximidade crítica de prazo.
 * Trafegado na exchange 'govflow.events' sob a routing-key 'transferegov.prazo.alerta'.
 */
public record AlertaPrazoExpirandoEvent(
        String nrConvenio,
        String municipio,
        String uf,
        String cnpjProponente,
        String nomeProponente,
        TipoPrazo tipoPrazo,
        LocalDate dataVencimento,
        Long diasRestantes,
        NivelRisco nivelRisco,
        BigDecimal valorGlobal,
        OffsetDateTime emitidoEm
) {
    public static AlertaPrazoExpirandoEvent of(
            String nrConvenio,
            String municipio,
            String uf,
            String cnpjProponente,
            String nomeProponente,
            TipoPrazo tipoPrazo,
            LocalDate dataVencimento,
            Long diasRestantes,
            NivelRisco nivelRisco,
            BigDecimal valorGlobal
    ) {
        return new AlertaPrazoExpirandoEvent(
                nrConvenio,
                municipio,
                uf,
                cnpjProponente,
                nomeProponente,
                tipoPrazo,
                dataVencimento,
                diasRestantes,
                nivelRisco,
                valorGlobal,
                OffsetDateTime.now()
        );
    }
}
