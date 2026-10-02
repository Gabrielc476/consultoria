package br.com.govflow.core.application.port.out;

import java.io.InputStream;
import java.util.Optional;

public interface DocumentoStoragePort {

    Optional<InputStream> carregarArquivo(String bucket, String s3Key);

    String salvarArquivo(String bucket, String s3Key, InputStream inputStream, long tamanhoBytes, String contentType);

    String gerarPresignedUrlPreview(String bucket, String s3Key, int expirationMinutes);

    String gerarPresignedUrlDownload(String bucket, String s3Key, String nomeArquivoOriginal, int expirationMinutes);

    void excluirArquivo(String bucket, String s3Key);
}
