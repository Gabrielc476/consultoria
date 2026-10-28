package br.com.govflow.core.application.port.in;

import br.com.govflow.core.domain.model.licitacao.Licitacao;

import java.time.LocalDate;
import java.util.UUID;

public interface EmitirAioUseCase {

    Licitacao emitir(EmitirAioCommand command);

    record EmitirAioCommand(
            UUID convenioId,
            UUID licitacaoId,
            String numeroAio,
            LocalDate dataEmissaoAio
    ) {}
}
