package br.com.govflow.core.application.port.in;

import br.com.govflow.core.domain.model.convenio.Convenio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public interface CadastrarConvenioUseCase {

    Convenio cadastrar(CadastrarConvenioCommand command);

    record CadastrarConvenioCommand(
            UUID tenantId,
            UUID prefeituraId,
            String numeroSiconv,
            String numeroProcesso,
            String orgaoConcedente,
            String objeto,
            BigDecimal valorGlobal,
            BigDecimal valorRepasse,
            BigDecimal valorContrapartida,
            boolean possuiClausulaSuspensiva,
            LocalDate prazoClausulaSuspensiva,
            LocalDate dataInicioVigencia,
            LocalDate dataFimVigencia
    ) {}
}
