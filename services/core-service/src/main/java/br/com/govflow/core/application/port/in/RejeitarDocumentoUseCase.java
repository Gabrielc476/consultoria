package br.com.govflow.core.application.port.in;

import br.com.govflow.core.domain.model.AuditoriaRevisao;
import br.com.govflow.core.domain.model.Documento;

import java.util.UUID;

public interface RejeitarDocumentoUseCase {

    record RejeitarDocumentoCommand(
            UUID documentoId,
            UUID analistaId,
            String motivo
    ) {
    }

    record ResultadoRejeicao(
            Documento documento,
            AuditoriaRevisao auditoria
    ) {
    }

    ResultadoRejeicao rejeitar(RejeitarDocumentoCommand command);
}
