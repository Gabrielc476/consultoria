package br.com.govflow.transferegov.event.model;

import br.com.govflow.transferegov.domain.compliance.StatusAdpf854;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AlertaInconformidadeAdpf854Event(
        UUID eventId,
        Long idPlanoAcao,
        String codigoPlanoAcao,
        String municipio,
        String uf,
        String cnpjBeneficiario,
        String nomeParlamentar,
        Integer anoEmenda,
        BigDecimal valorTotal,
        StatusAdpf854 statusAdpf854,
        String tipoInconformidade,
        String severidade,
        String descricao,
        OffsetDateTime timestamp
) {
    public static AlertaInconformidadeAdpf854Event of(
            Long idPlanoAcao,
            String codigoPlanoAcao,
            String municipio,
            String uf,
            String cnpjBeneficiario,
            String nomeParlamentar,
            Integer anoEmenda,
            BigDecimal valorTotal,
            StatusAdpf854 statusAdpf854,
            String tipoInconformidade,
            String severidade,
            String descricao
    ) {
        return new AlertaInconformidadeAdpf854Event(
                UUID.randomUUID(),
                idPlanoAcao,
                codigoPlanoAcao,
                municipio,
                uf,
                cnpjBeneficiario,
                nomeParlamentar,
                anoEmenda,
                valorTotal,
                statusAdpf854,
                tipoInconformidade,
                severidade,
                descricao,
                OffsetDateTime.now()
        );
    }
}
