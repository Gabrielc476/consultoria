package br.com.govflow.core.domain.model;

/**
 * Value Object que encapsula as informações de armazenamento do arquivo em storage de objetos (S3/MinIO).
 * Elimina o code smell de Data Clumps no Agregado Documento.
 */
public record ArmazenamentoArquivo(
        String s3Bucket,
        String s3Key,
        String nomeArquivoOriginal,
        String contentType,
        Long tamanhoBytes
) {
    public static ArmazenamentoArquivo of(String s3Bucket,
                                         String s3Key,
                                         String nomeArquivoOriginal,
                                         String contentType,
                                         Long tamanhoBytes) {
        return new ArmazenamentoArquivo(s3Bucket, s3Key, nomeArquivoOriginal, contentType, tamanhoBytes);
    }
}
