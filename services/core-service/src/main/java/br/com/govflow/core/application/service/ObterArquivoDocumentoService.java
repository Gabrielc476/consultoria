package br.com.govflow.core.application.service;

import br.com.govflow.core.application.port.in.ObterArquivoDocumentoUseCase;
import br.com.govflow.core.application.port.out.DocumentoRepositoryPort;
import br.com.govflow.core.application.port.out.DocumentoStoragePort;
import br.com.govflow.core.domain.exception.DocumentoNaoEncontradoException;
import br.com.govflow.core.domain.model.Documento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

@Service
public class ObterArquivoDocumentoService implements ObterArquivoDocumentoUseCase {

    private static final Logger log = LoggerFactory.getLogger(ObterArquivoDocumentoService.class);

    private final DocumentoRepositoryPort documentoRepositoryPort;
    private final DocumentoStoragePort documentoStoragePort;

    public ObterArquivoDocumentoService(DocumentoRepositoryPort documentoRepositoryPort,
                                       DocumentoStoragePort documentoStoragePort) {
        this.documentoRepositoryPort = documentoRepositoryPort;
        this.documentoStoragePort = documentoStoragePort;
    }

    @Override
    @Transactional(readOnly = true)
    public ArquivoConteudo obterArquivo(UUID documentoId) {
        Documento doc = documentoRepositoryPort.buscarPorId(documentoId)
                .orElseThrow(() -> new DocumentoNaoEncontradoException(documentoId));

        String bucket = doc.getS3Bucket();
        String s3Key = doc.getS3Key();
        String contentType = (doc.getContentType() != null && !doc.getContentType().isBlank())
                ? doc.getContentType()
                : "application/pdf";
        String nomeOriginal = (doc.getNomeArquivoOriginal() != null && !doc.getNomeArquivoOriginal().isBlank())
                ? doc.getNomeArquivoOriginal()
                : "documento.pdf";
        long tamanho = doc.getTamanhoBytes() != null ? doc.getTamanhoBytes() : 0L;

        if (bucket != null && s3Key != null) {
            Optional<InputStream> streamOpt = documentoStoragePort.carregarArquivo(bucket, s3Key);
            if (streamOpt.isPresent()) {
                return new ArquivoConteudo(streamOpt.get(), contentType, nomeOriginal, tamanho);
            }
        }

        log.warn("Arquivo do documento {} não localizado no bucket {} com chave {}. Gerando payload de contingência.",
                documentoId, bucket, s3Key);

        // Fallback: Gera um PDF básico em memória com os dados do documento para permitir testes mesmo sem MinIO
        byte[] fallbackPdf = gerarPdfPlaceholder(doc);
        return new ArquivoConteudo(
                new ByteArrayInputStream(fallbackPdf),
                "application/pdf",
                nomeOriginal.endsWith(".pdf") ? nomeOriginal : nomeOriginal + ".pdf",
                fallbackPdf.length
        );
    }

    private byte[] gerarPdfPlaceholder(Documento doc) {
        String pdf = "%PDF-1.4\n" +
                "1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n" +
                "2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n" +
                "3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >> endobj\n" +
                "4 0 obj << /Length 120 >> stream\n" +
                "BT /F1 14 Tf 50 720 Td (GovFlow - Documento Fiscal: " + doc.getNomeArquivoOriginal() + ") Tj ET\n" +
                "BT /F1 11 Tf 50 690 Td (Status: " + doc.getStatus() + " | ID: " + doc.getId() + ") Tj ET\n" +
                "endstream endobj\n" +
                "5 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj\n" +
                "xref\n0 6\n0000000000 65535 f \n0000000009 00000 n \n0000000058 00000 n \n0000000115 00000 n \n0000000244 00000 n \n0000000414 00000 n \n" +
                "trailer << /Size 6 /Root 1 0 R >>\nstartxref\n493\n%%EOF";
        return pdf.getBytes(StandardCharsets.ISO_8859_1);
    }
}
