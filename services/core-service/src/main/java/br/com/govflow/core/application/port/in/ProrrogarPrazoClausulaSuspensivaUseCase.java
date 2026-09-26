package br.com.govflow.core.application.port.in;

import br.com.govflow.core.domain.model.convenio.Convenio;

import java.time.LocalDate;
import java.util.UUID;

public interface ProrrogarPrazoClausulaSuspensivaUseCase {

    Convenio solicitarProrrogacao(UUID convenioId, LocalDate novoPrazo);
}
