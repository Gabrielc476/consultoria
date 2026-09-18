package br.com.govflow.core.domain.exception;

import br.com.govflow.core.domain.model.StatusDocumento;

public class DocumentoEstadoInvalidoException extends DomainException {

    public DocumentoEstadoInvalidoException(StatusDocumento statusAtual, String acaoDesejada) {
        super("DOCUMENTO_ESTADO_INVALIDO",
                String.format("Operação '%s' não permitida para o documento no estado atual '%s'.", acaoDesejada, statusAtual));
    }

    public DocumentoEstadoInvalidoException(String mensagem) {
        super("DOCUMENTO_ESTADO_INVALIDO", mensagem);
    }
}
