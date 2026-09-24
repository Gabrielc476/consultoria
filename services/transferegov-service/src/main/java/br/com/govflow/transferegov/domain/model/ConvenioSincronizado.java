package br.com.govflow.transferegov.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ConvenioSincronizado(
        String nrConvenio,
        String idProposta,
        String cnpjProponente,
        String nomeProponente,
        String municipio,
        String uf,
        String situacaoConvenio,
        boolean instrumentoAtivo,
        LocalDate dataInicioVigencia,
        LocalDate dataFimVigencia,
        LocalDate dataLimitePrestacaoContas,
        LocalDate dataSuspensiva,
        BigDecimal valorGlobal,
        BigDecimal valorRepasse,
        BigDecimal valorContrapartida,
        BigDecimal valorSaldoConta,
        String objeto,
        String dataCargaSiconv
) {}
