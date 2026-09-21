package br.com.govflow.core.application.port.out;

import java.io.InputStream;
import java.util.Optional;

public interface DocumentoStoragePort {
    Optional<InputStream> carregarArquivo(String bucket, String s3Key);
}
