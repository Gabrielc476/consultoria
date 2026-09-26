package br.com.govflow.core.application.port.out;

import java.io.InputStream;
import java.util.Optional;

public interface ClausulaSuspensivaStoragePort {

    String salvarArquivo(String bucket, String s3Key, byte[] conteudo, String contentType);

    Optional<InputStream> carregarArquivo(String bucket, String s3Key);
}
