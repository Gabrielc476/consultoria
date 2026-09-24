package br.com.govflow.transferegov.sync.client;

import java.io.IOException;
import java.io.InputStream;

public interface SiconvStreamingClient {

    /**
     * Abre uma conexão de streaming direta com o arquivo remoto (ZIP)
     * sem gravá-lo no disco local e sem carregar todo o seu conteúdo na memória.
     *
     * @param fileName Nome do arquivo remoto (ex: "siconv_convenio.zip")
     * @return InputStream para leitura contínua dos bytes
     * @throws IOException Caso ocorra falha de rede ou timeout
     */
    InputStream openZipStream(String fileName) throws IOException;
}
