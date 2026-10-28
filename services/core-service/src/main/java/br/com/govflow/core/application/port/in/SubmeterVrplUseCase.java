package br.com.govflow.core.application.port.in;

import br.com.govflow.core.domain.model.licitacao.Licitacao;

import java.time.LocalDate;
import java.util.UUID;

public interface SubmeterVrplUseCase {

    Licitacao submeter(SubmeterVrplCommand command);

    record SubmeterVrplCommand(
            UUID convenioId,
            UUID licitacaoId,
            String numeroVrplTransferegov,
            LocalDate dataEnvioVrpl
    ) {}
}
