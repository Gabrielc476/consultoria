package br.com.govflow.core.application.port.out;

import br.com.govflow.core.domain.event.DocumentoProntoParaTransferegovEvent;
import br.com.govflow.core.domain.event.DocumentoRejeitadoEvent;

public interface DocumentoEventPublisherPort {

    void publicarDocumentoPronto(DocumentoProntoParaTransferegovEvent evento);

    void publicarDocumentoRejeitado(DocumentoRejeitadoEvent evento);
}
