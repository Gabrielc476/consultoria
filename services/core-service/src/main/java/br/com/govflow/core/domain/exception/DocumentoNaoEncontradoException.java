package br.com.govflow.core.domain.exception;

import java.util.UUID;

public class DocumentoNaoEncontradoException extends DomainException {

    public DocumentoNaoEncontradoException(UUID id) {
        super("DOCUMENTO_NAO_ENCONTRADO", "Documento não encontrado com o ID: " + id);
    }
}
