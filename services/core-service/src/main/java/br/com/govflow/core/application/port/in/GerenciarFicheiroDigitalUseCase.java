package br.com.govflow.core.application.port.in;

import br.com.govflow.core.domain.model.Documento;
import br.com.govflow.core.domain.model.documento.CategoriaDocumento;
import br.com.govflow.core.domain.model.documento.FaseCicloVida;
import br.com.govflow.core.domain.model.documento.FicheiroDigital;

import java.util.List;
import java.util.UUID;

public interface GerenciarFicheiroDigitalUseCase {

    record UploadDocumentoCommand(
            UUID convenioId,
            FaseCicloVida fase,
            CategoriaDocumento categoria,
            String pastaVirtual,
            String nomeArquivo,
            String contentType,
            byte[] conteudo,
            List<String> tags,
            UUID usuarioId
    ) {}

    record MoverDocumentoCommand(
            UUID documentoId,
            FaseCicloVida novaFase,
            String novaPastaVirtual,
            String justificativa,
            UUID usuarioId
    ) {}

    record ExcluirDocumentoCommand(
            UUID documentoId,
            String justificativa,
            UUID usuarioId
    ) {}

    FicheiroDigital obterFicheiro(UUID convenioId);

    List<Documento> listarDocumentosFase(UUID convenioId, FaseCicloVida fase);

    Documento uploadDocumento(UploadDocumentoCommand command);

    Documento moverDocumento(MoverDocumentoCommand command);

    String obterUrlPreview(UUID documentoId);

    Documento obterDocumentoPorId(UUID documentoId);

    void escreverConteudoDocumento(UUID documentoId, java.io.OutputStream outputStream);

    void excluirDocumento(ExcluirDocumentoCommand command);

    List<br.com.govflow.core.domain.model.documento.DocumentoAuditoria> listarAuditoria(UUID documentoId);
}
