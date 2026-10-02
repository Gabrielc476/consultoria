package br.com.govflow.core.infrastructure.adapter.out.storage;

import br.com.govflow.core.application.port.out.DocumentoStoragePort;
import br.com.govflow.core.domain.model.documento.FaseCicloVida;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Component
public class MinioDocumentoStorageAdapter implements DocumentoStoragePort {

    private static final Logger log = LoggerFactory.getLogger(MinioDocumentoStorageAdapter.class);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String defaultBucket;

    public MinioDocumentoStorageAdapter(
            S3Client s3Client,
            S3Presigner s3Presigner,
            @Value("${aws.s3.bucket-documentos:${govflow.storage.bucket-documentos:govflow-documentos}}") String defaultBucket) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.defaultBucket = defaultBucket != null && !defaultBucket.trim().isEmpty() ? defaultBucket : "govflow-documentos";
    }

    public static String construirS3Key(UUID tenantId, UUID prefeituraId, UUID convenioId, FaseCicloVida fase, UUID docId, String nomeOriginal) {
        String sanitizado = (nomeOriginal != null) ? nomeOriginal.replaceAll("[^a-zA-Z0-9._-]", "_") : "arquivo.pdf";
        String fasePasta = (fase != null) ? fase.getNomePasta() : "05_Execucao_Financeira_e_Pagamentos";
        return String.format("tenants/%s/prefeituras/%s/convenios/%s/fases/%s/%s_%s",
                tenantId,
                prefeituraId != null ? prefeituraId : "sem-prefeitura",
                convenioId != null ? convenioId : "sem-convenio",
                fasePasta,
                docId != null ? docId : UUID.randomUUID(),
                sanitizado);
    }

    public static String calcularSha256(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(bytes);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Algoritmo SHA-256 indisponível", e);
        }
    }

    @Override
    public Optional<InputStream> carregarArquivo(String bucket, String s3Key) {
        String bucketAlvo = (bucket != null && !bucket.isBlank()) ? bucket : defaultBucket;
        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucketAlvo)
                    .key(s3Key)
                    .build();

            ResponseInputStream<GetObjectResponse> responseStream = s3Client.getObject(getRequest);
            return Optional.of(responseStream);
        } catch (NoSuchKeyException e) {
            log.warn("Objeto não encontrado no S3: bucket={}, key={}", bucketAlvo, s3Key);
            return Optional.empty();
        } catch (S3Exception e) {
            log.error("Erro do S3 ao carregar objeto: bucket={}, key={}, erro={}", bucketAlvo, s3Key,
                    e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.error("Erro inesperado ao buscar arquivo no storage: bucket={}, key={}", bucketAlvo, s3Key, e);
            return Optional.empty();
        }
    }

    @Override
    public String salvarArquivo(String bucket, String s3Key, InputStream inputStream, long tamanhoBytes, String contentType) {
        String bucketAlvo = (bucket != null && !bucket.isBlank()) ? bucket : defaultBucket;
        try {
            garantirBucketExistente(bucketAlvo);

            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketAlvo)
                    .key(s3Key)
                    .contentType(contentType != null ? contentType : "application/octet-stream")
                    .contentLength(tamanhoBytes)
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromInputStream(inputStream, tamanhoBytes));
            log.info("Arquivo gravado com sucesso no S3/MinIO: bucket={}, key={}, tamanho={}", bucketAlvo, s3Key, tamanhoBytes);
            return s3Key;
        } catch (Exception e) {
            log.error("Erro ao gravar arquivo no S3/MinIO: bucket={}, key={}", bucketAlvo, s3Key, e);
            throw new RuntimeException("Falha ao salvar arquivo no storage: " + e.getMessage(), e);
        }
    }

    @Override
    public String gerarPresignedUrlPreview(String bucket, String s3Key, int expirationMinutes) {
        String bucketAlvo = (bucket != null && !bucket.isBlank()) ? bucket : defaultBucket;
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketAlvo)
                    .key(s3Key)
                    .responseContentDisposition("inline")
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(expirationMinutes > 0 ? expirationMinutes : 15))
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
            return presigned.url().toString();
        } catch (Exception e) {
            log.error("Erro ao gerar presigned preview URL: bucket={}, key={}", bucketAlvo, s3Key, e);
            throw new RuntimeException("Falha ao gerar URL de pré-visualização: " + e.getMessage(), e);
        }
    }

    @Override
    public String gerarPresignedUrlDownload(String bucket, String s3Key, String nomeArquivoOriginal, int expirationMinutes) {
        String bucketAlvo = (bucket != null && !bucket.isBlank()) ? bucket : defaultBucket;
        try {
            String disposition = "attachment; filename=\"" + (nomeArquivoOriginal != null ? nomeArquivoOriginal.replace("\"", "") : "arquivo") + "\"";
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketAlvo)
                    .key(s3Key)
                    .responseContentDisposition(disposition)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(expirationMinutes > 0 ? expirationMinutes : 15))
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
            return presigned.url().toString();
        } catch (Exception e) {
            log.error("Erro ao gerar presigned download URL: bucket={}, key={}", bucketAlvo, s3Key, e);
            throw new RuntimeException("Falha ao gerar URL de download: " + e.getMessage(), e);
        }
    }

    @Override
    public void excluirArquivo(String bucket, String s3Key) {
        String bucketAlvo = (bucket != null && !bucket.isBlank()) ? bucket : defaultBucket;
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketAlvo)
                    .key(s3Key)
                    .build());
            log.info("Arquivo excluído do S3/MinIO: bucket={}, key={}", bucketAlvo, s3Key);
        } catch (Exception e) {
            log.error("Erro ao excluir arquivo no S3/MinIO: bucket={}, key={}", bucketAlvo, s3Key, e);
            throw new RuntimeException("Falha ao excluir arquivo do storage: " + e.getMessage(), e);
        }
    }

    private void garantirBucketExistente(String bucket) {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (NoSuchBucketException e) {
            log.info("Bucket {} não existe no MinIO. Criando bucket...", bucket);
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
        } catch (Exception e) {
            if (e instanceof S3Exception s3e && s3e.statusCode() == 404) {
                log.info("Bucket {} não encontrado (404). Criando...", bucket);
                s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            } else {
                log.warn("Não foi possível verificar status do bucket {}: {}", bucket, e.getMessage());
            }
        }
    }
}
