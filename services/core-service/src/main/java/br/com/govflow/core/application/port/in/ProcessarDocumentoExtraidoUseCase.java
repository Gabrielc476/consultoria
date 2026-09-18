package br.com.govflow.core.application.port.in;

import br.com.govflow.core.domain.model.BoundingBox;
import br.com.govflow.core.domain.model.Documento;
import br.com.govflow.core.domain.model.ExtracaoSugerida;

import java.util.Map;
import java.util.UUID;

public interface ProcessarDocumentoExtraidoUseCase {

    record ProcessarDocumentoExtraidoCommand(
            UUID tenantId,
            UUID documentoId,
            UUID prefeituraId,
            UUID convenioId,
            String s3Bucket,
            String s3Key,
            String nomeArquivoOriginal,
            String contentType,
            Long tamanhoBytes,
            ExtracaoSugerida extracao,
            Map<String, BoundingBox> boundingBoxes
    ) {
    }

    Documento processar(ProcessarDocumentoExtraidoCommand command);
}
