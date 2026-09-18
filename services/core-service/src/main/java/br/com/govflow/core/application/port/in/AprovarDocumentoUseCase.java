package br.com.govflow.core.application.port.in;

import br.com.govflow.core.domain.model.AuditoriaRevisao;
import br.com.govflow.core.domain.model.DadosRevisaoAnalista;
import br.com.govflow.core.domain.model.Documento;

import java.util.UUID;

public interface AprovarDocumentoUseCase {

    record AprovarDocumentoCommand(
            UUID documentoId,
            UUID analistaId,
            DadosRevisaoAnalista revisao,
            String observacao
    ) {
    }

    record ResultadoAprovacao(
            Documento documento,
            AuditoriaRevisao auditoria
    ) {
    }

    ResultadoAprovacao aprovar(AprovarDocumentoCommand command);
}
