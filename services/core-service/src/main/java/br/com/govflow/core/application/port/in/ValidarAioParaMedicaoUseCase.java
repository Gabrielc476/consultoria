package br.com.govflow.core.application.port.in;

import java.util.UUID;

public interface ValidarAioParaMedicaoUseCase {

    void garantirAioEmitida(UUID licitacaoId);
}
