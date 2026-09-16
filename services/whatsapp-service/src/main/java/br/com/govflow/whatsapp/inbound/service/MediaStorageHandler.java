package br.com.govflow.whatsapp.inbound.service;

import br.com.govflow.whatsapp.config.WhatsAppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Base64;
import java.util.UUID;

@Service
public class MediaStorageHandler {

    private static final Logger log = LoggerFactory.getLogger(MediaStorageHandler.class);

    private final S3Client s3Client;
    private final WhatsAppProperties properties;
    private final HttpClient httpClient;

    public MediaStorageHandler(S3Client s3Client, WhatsAppProperties properties) {
        this.s3Client = s3Client;
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public record StorageResult(
            String bucket,
            String key,
            long sizeBytes,
            String mimeType
    ) {}

    public StorageResult streamAndStore(
            String mediaSource,
            String mimeType,
            String originalFileName,
            Long declaredSize,
            UUID prefeituraId,
            UUID messageId
    ) {
        String bucket = properties.getS3().getBucketDocuments();
        String s3Key = generateS3Key(prefeituraId, messageId, originalFileName);
        String finalMimeType = (mimeType != null && !mimeType.isBlank()) ? mimeType : "application/octet-stream";

        log.info("Iniciando streaming direto de mídia para MinIO S3: bucket={}, key={}", bucket, s3Key);

        try {
            if (mediaSource != null && (mediaSource.startsWith("http://") || mediaSource.startsWith("https://"))) {
                return streamFromHttpUrl(mediaSource, bucket, s3Key, finalMimeType, declaredSize);
            } else if (mediaSource != null && mediaSource.startsWith("data:")) {
                return streamFromBase64DataUri(mediaSource, bucket, s3Key, finalMimeType);
            } else if (mediaSource != null && isBase64(mediaSource)) {
                byte[] decoded = Base64.getDecoder().decode(mediaSource);
                return storeFromInputStream(new ByteArrayInputStream(decoded), decoded.length, bucket, s3Key, finalMimeType);
            } else {
                // Caso não haja URL ou base64 válida, gera objeto vazio placeholder para não abortar auditoria
                byte[] empty = new byte[0];
                return storeFromInputStream(new ByteArrayInputStream(empty), 0, bucket, s3Key, finalMimeType);
            }
        } catch (Exception e) {
            log.error("Falha ao efetuar streaming de mídia para o S3: key={}", s3Key, e);
            throw new RuntimeException("Erro ao transferir arquivo para o MinIO: " + e.getMessage(), e);
        }
    }

    public StorageResult storeFromInputStream(
            InputStream inputStream,
            long contentLength,
            String bucket,
            String s3Key,
            String mimeType
    ) {
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .contentType(mimeType)
                .build();

        s3Client.putObject(putRequest, RequestBody.fromInputStream(inputStream, contentLength));
        log.info("Arquivo gravado no S3 com sucesso via streaming: {} ({} bytes)", s3Key, contentLength);

        return new StorageResult(bucket, s3Key, contentLength, mimeType);
    }

    private StorageResult streamFromHttpUrl(
            String httpUrl,
            String bucket,
            String s3Key,
            String mimeType,
            Long declaredSize
    ) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(httpUrl))
                .timeout(Duration.ofSeconds(60))
                .GET()
                .build();

        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() >= 400) {
            throw new IllegalStateException("Falha ao baixar mídia da Evolution API. HTTP Status: " + response.statusCode());
        }

        long length = response.headers().firstValueAsLong("Content-Length")
                .orElse(declaredSize != null && declaredSize > 0 ? declaredSize : -1L);

        try (InputStream stream = response.body()) {
            if (length > 0) {
                return storeFromInputStream(stream, length, bucket, s3Key, mimeType);
            } else {
                // Se o servidor HTTP não enviou Content-Length e nem o DTO tinha, lê em memória de forma segura
                byte[] bytes = stream.readAllBytes();
                return storeFromInputStream(new ByteArrayInputStream(bytes), bytes.length, bucket, s3Key, mimeType);
            }
        }
    }

    private StorageResult streamFromBase64DataUri(
            String dataUri,
            String bucket,
            String s3Key,
            String defaultMimeType
    ) {
        String[] parts = dataUri.split(",");
        String base64Data = parts.length > 1 ? parts[1] : parts[0];
        String detectedMime = defaultMimeType;
        if (parts[0].contains(":") && parts[0].contains(";")) {
            detectedMime = parts[0].substring(parts[0].indexOf(":") + 1, parts[0].indexOf(";"));
        }

        byte[] decoded = Base64.getDecoder().decode(base64Data);
        return storeFromInputStream(new ByteArrayInputStream(decoded), decoded.length, bucket, s3Key, detectedMime);
    }

    public String generateS3Key(UUID prefeituraId, UUID messageId, String rawFilename) {
        String entityFolder = (prefeituraId != null) ? prefeituraId.toString() : "unidentified";
        String year = String.valueOf(LocalDate.now().getYear());
        String month = String.format("%02d", LocalDate.now().getMonthValue());
        String cleanName = sanitizeFilename(rawFilename);

        return String.format("raw/whatsapp/%s/%s/%s/%s_%s", entityFolder, year, month, messageId, cleanName);
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "documento.bin";
        }
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private boolean isBase64(String str) {
        if (str == null || str.length() < 10) return false;
        try {
            Base64.getDecoder().decode(str.substring(0, Math.min(100, str.length())));
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
