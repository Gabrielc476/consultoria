package br.com.govflow.transferegov.sync.client;

import br.com.govflow.transferegov.config.SiconvProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

@Component
@Primary
public class HttpSiconvStreamingClient implements SiconvStreamingClient {

    private static final Logger log = LoggerFactory.getLogger(HttpSiconvStreamingClient.class);

    private final SiconvProperties properties;

    public HttpSiconvStreamingClient(SiconvProperties properties) {
        this.properties = properties;
    }

    @Override
    public InputStream openZipStream(String fileName) throws IOException {
        String fullUrl = properties.baseUrl().replaceAll("/+$", "") + "/" + fileName;
        log.info("Iniciando conexão de streaming com dump remoto SICONV: {}", fullUrl);

        URL url = URI.create(fullUrl).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(properties.connectTimeoutMs());
        connection.setReadTimeout(properties.readTimeoutMs());
        connection.setRequestProperty("User-Agent", "GovFlow-Transferegov-Sync/1.0");
        connection.setRequestProperty("Accept", "application/zip, application/octet-stream, */*");
        connection.setInstanceFollowRedirects(true);

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            connection.disconnect();
            throw new IOException("Falha ao abrir stream para " + fullUrl + ". HTTP status: " + responseCode);
        }

        return connection.getInputStream();
    }
}
