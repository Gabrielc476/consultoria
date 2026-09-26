package br.com.govflow.core.infrastructure.adapter.out.storage;

import br.com.govflow.core.application.port.out.ClausulaSuspensivaStoragePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.InputStream;
import java.util.Optional;

@Component
public class MinioClausulaSuspensivaStorageAdapter implements ClausulaSuspensivaStoragePort {

    private static final Logger log = LoggerFactory.getLogger(MinioClausulaSuspensivaStorageAdapter.class);

    private final S3Client s3Client;

    public MinioClausulaSuspensivaStorageAdapter(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public String salvarArquivo(String bucket, String s3Key, byte[] conteudo, String contentType) {
        try {
            garantirBucketExistente(bucket);

            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .contentType(contentType != null ? contentType : "application/octet-stream")
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromBytes(conteudo));
            log.info("Arquivo gravado com sucesso no S3/MinIO: bucket={}, key={}", bucket, s3Key);
            return s3Key;
        } catch (Exception e) {
            log.error("Erro ao gravar arquivo no S3/MinIO: bucket={}, key={}", bucket, s3Key, e);
            throw new RuntimeException("Falha ao salvar arquivo no storage: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<InputStream> carregarArquivo(String bucket, String s3Key) {
        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .build();

            ResponseInputStream<GetObjectResponse> responseStream = s3Client.getObject(getRequest);
            return Optional.of(responseStream);
        } catch (NoSuchKeyException e) {
            log.warn("Objeto não encontrado no S3: bucket={}, key={}", bucket, s3Key);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Erro inesperado ao buscar arquivo no S3: bucket={}, key={}", bucket, s3Key, e);
            return Optional.empty();
        }
    }

    private void garantirBucketExistente(String bucket) {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (NoSuchBucketException e) {
            log.info("Bucket {} não existe no MinIO. Criando bucket...", bucket);
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
        } catch (Exception e) {
            // Em caso de erro 404 via S3Exception
            if (e instanceof S3Exception s3e && s3e.statusCode() == 404) {
                log.info("Bucket {} não encontrado (404). Criando...", bucket);
                s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            } else {
                log.warn("Não foi possível verificar status do bucket {}: {}", bucket, e.getMessage());
            }
        }
    }
}
