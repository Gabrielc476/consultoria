package br.com.govflow.whatsapp.inbound;

import br.com.govflow.whatsapp.config.WhatsAppProperties;
import br.com.govflow.whatsapp.inbound.service.MediaStorageHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaStorageHandlerTest {

    @Mock
    private S3Client s3Client;

    private WhatsAppProperties properties;
    private MediaStorageHandler mediaStorageHandler;

    private final UUID prefeituraId = UUID.randomUUID();
    private final UUID messageId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        properties = new WhatsAppProperties();
        properties.getS3().setBucketDocuments("govflow-documents");
        mediaStorageHandler = new MediaStorageHandler(s3Client, properties);
    }

    @Test
    @DisplayName("Deve gerar S3 key estruturada e segura com prefeitura, ano, mês e UUID")
    void deveGerarS3KeySegura() {
        String key = mediaStorageHandler.generateS3Key(prefeituraId, messageId, "Nota Fiscal Medição #01.pdf");

        assertTrue(key.startsWith("raw/whatsapp/" + prefeituraId + "/"));
        assertTrue(key.endsWith(messageId + "_Nota_Fiscal_Medi__o__01.pdf"));
    }

    @Test
    @DisplayName("Deve usar pasta unidentified quando a prefeitura não for resolvida")
    void deveUsarPastaUnidentifiedQuandoPrefeituraNula() {
        String key = mediaStorageHandler.generateS3Key(null, messageId, "documento.pdf");

        assertTrue(key.startsWith("raw/whatsapp/unidentified/"));
        assertTrue(key.contains(messageId.toString()));
    }

    @Test
    @DisplayName("Deve transferir stream diretamente para o MinIO S3 sem acumular em disco")
    void deveTransferirStreamParaS3SemGravarEmDisco() {
        byte[] content = "Conteúdo simulado de nota fiscal PDF".getBytes();
        InputStream is = new ByteArrayInputStream(content);
        String s3Key = "raw/whatsapp/test/doc.pdf";

        MediaStorageHandler.StorageResult result = mediaStorageHandler.storeFromInputStream(
                is, content.length, "govflow-documents", s3Key, "application/pdf"
        );

        assertEquals("govflow-documents", result.bucket());
        assertEquals(s3Key, result.key());
        assertEquals(content.length, result.sizeBytes());

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client, times(1)).putObject(requestCaptor.capture(), any(RequestBody.class));

        PutObjectRequest captured = requestCaptor.getValue();
        assertEquals("govflow-documents", captured.bucket());
        assertEquals(s3Key, captured.key());
        assertEquals("application/pdf", captured.contentType());
    }
}
