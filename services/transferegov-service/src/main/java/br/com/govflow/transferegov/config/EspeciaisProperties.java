package br.com.govflow.transferegov.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collections;
import java.util.Set;

@ConfigurationProperties(prefix = "govflow.transferegov.especiais")
public record EspeciaisProperties(
        String baseUrl,
        int connectTimeoutMs,
        int readTimeoutMs,
        int pageSize,
        int maxPagesPerSync,
        String ufFilter,
        long rateLimitDelayMs,
        int maxRetries,
        String cron,
        Set<String> targetCnpjs
) {
    public EspeciaisProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api-publica.transferegov.gestao.gov.br/especiais";
        }
        if (connectTimeoutMs <= 0) {
            connectTimeoutMs = 10000;
        }
        if (readTimeoutMs <= 0) {
            readTimeoutMs = 30000;
        }
        if (pageSize <= 0) {
            pageSize = 50;
        }
        if (maxPagesPerSync <= 0) {
            maxPagesPerSync = 100;
        }
        if (ufFilter == null || ufFilter.isBlank()) {
            ufFilter = "PB";
        }
        if (rateLimitDelayMs < 0) {
            rateLimitDelayMs = 100;
        }
        if (maxRetries <= 0) {
            maxRetries = 3;
        }
        if (cron == null || cron.isBlank()) {
            cron = "0 30 7 * * *";
        }
        if (targetCnpjs == null) {
            targetCnpjs = Collections.emptySet();
        }
    }
}
