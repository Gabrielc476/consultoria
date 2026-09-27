package br.com.govflow.core.application.port.out;

import java.io.InputStream;
import java.util.Optional;

public interface ClausulaSuspensivaStoragePort {

    /**
     * Salva o arquivo no storage utilizando o bucket padrão encapsulado pelo adaptador.
     */
    String salvarArquivo(String s3Key, byte[] conteudo, String contentType);

    /**
     * Carrega os bytes do arquivo para download direto via API REST.
     */
    Optional<byte[]> carregarArquivoBytes(String s3Key);

    /**
     * Carrega a stream do arquivo utilizando o bucket padrão.
     */
    Optional<InputStream> carregarArquivo(String s3Key);

    /**
     * Sobrecarga legada com bucket explícito.
     */
    String salvarArquivo(String bucket, String s3Key, byte[] conteudo, String contentType);

    Optional<InputStream> carregarArquivo(String bucket, String s3Key);
}
