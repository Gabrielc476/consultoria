package br.com.govflow.core.application.port.in;

import java.io.InputStream;
import java.util.UUID;

public interface ObterArquivoDocumentoUseCase {

    record ArquivoConteudo(
            InputStream inputStream,
            String contentType,
            String nomeArquivoOriginal,
            long tamanhoBytes
    ) {}

    ArquivoConteudo obterArquivo(UUID documentoId);
}
