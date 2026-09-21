package br.com.govflow.core.infrastructure.adapter.out.storage;

import br.com.govflow.core.application.port.out.DocumentoStoragePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.InputStream;
import java.util.Optional;

@Component
public class MinioDocumentoStorageAdapter implements DocumentoStoragePort {

    private static final Logger log = LoggerFactory.getLogger(MinioDocumentoStorageAdapter.class);

    private final S3Client s3Client;

    public MinioDocumentoStorageAdapter(S3Client s3Client) {
        this.s3Client = s3Client;
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
        } catch (S3Exception e) {
            log.error("Erro do S3 ao carregar objeto: bucket={}, key={}, erro={}", bucket, s3Key,
                    e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.error("Erro inesperado ao buscar arquivo no storage: bucket={}, key={}", bucket, s3Key, e);
            return Optional.empty();
        }
    }
}
