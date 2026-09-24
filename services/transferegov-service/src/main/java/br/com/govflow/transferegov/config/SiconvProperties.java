package br.com.govflow.transferegov.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collections;
import java.util.Set;

@ConfigurationProperties(prefix = "govflow.transferegov.siconv")
public record SiconvProperties(
        String baseUrl,
        String ufFilter,
        int connectTimeoutMs,
        int readTimeoutMs,
        String cron,
        int batchSize,
        Set<String> targetCnpjs
) {
    public SiconvProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api-publica.transferegov.gestao.gov.br/downloads/dadosgov";
        }
        if (ufFilter == null || ufFilter.isBlank()) {
            ufFilter = "PB";
        }
        if (connectTimeoutMs <= 0) {
            connectTimeoutMs = 15000;
        }
        if (readTimeoutMs <= 0) {
            readTimeoutMs = 60000;
        }
        if (batchSize <= 0) {
            batchSize = 200;
        }
        if (targetCnpjs == null) {
            targetCnpjs = Collections.emptySet();
        }
    }
}
