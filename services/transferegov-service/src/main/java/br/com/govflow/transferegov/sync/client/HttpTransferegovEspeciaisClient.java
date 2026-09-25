package br.com.govflow.transferegov.sync.client;

import br.com.govflow.transferegov.config.EspeciaisProperties;
import br.com.govflow.transferegov.sync.client.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.*;

@Component
public class HttpTransferegovEspeciaisClient implements TransferegovEspeciaisClient {

    private static final Logger log = LoggerFactory.getLogger(HttpTransferegovEspeciaisClient.class);

    private final RestClient restClient;
    private final EspeciaisProperties properties;

    public HttpTransferegovEspeciaisClient(
            @Qualifier("especiaisRestClient") RestClient restClient,
            EspeciaisProperties properties
    ) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public List<BeneficiarioEspecialDTO> consultarBeneficiarios(String uf, String cnpj) {
        Map<String, Object> params = new LinkedHashMap<>();
        if (uf != null && !uf.isBlank()) {
            params.put("uf_beneficiario", uf);
        }
        if (cnpj != null && !cnpj.isBlank()) {
            params.put("cnpj_beneficiario", cnpj);
        }

        return fetchAllPages(
                "/beneficiarios-especiais",
                params,
                new ParameterizedTypeReference<TransferegovPageResponse<BeneficiarioEspecialDTO>>() {}
        );
    }

    @Override
    public List<PlanoAcaoEspecialDTO> consultarPlanosAcao(Long idBeneficiario, Integer ano) {
        Map<String, Object> params = new LinkedHashMap<>();
        if (idBeneficiario != null) {
            params.put("id_beneficiario", idBeneficiario);
        }
        if (ano != null) {
            params.put("ano_plano_acao", ano);
        }

        return fetchAllPages(
                "/planos-acao-especiais",
                params,
                new ParameterizedTypeReference<TransferegovPageResponse<PlanoAcaoEspecialDTO>>() {}
        );
    }

    @Override
    public List<PlanoTrabalhoEspecialDTO> consultarPlanosTrabalho(Long idPlanoAcao) {
        return fetchPorPlanoAcao(
                "/planos-trabalho-especiais",
                idPlanoAcao,
                new ParameterizedTypeReference<TransferegovPageResponse<PlanoTrabalhoEspecialDTO>>() {}
        );
    }

    @Override
    public List<RelatorioGestaoEspecialDTO> consultarRelatoriosGestao(Long idPlanoAcao) {
        return fetchPorPlanoAcao(
                "/relatorios-gestao-novos-especiais",
                idPlanoAcao,
                new ParameterizedTypeReference<TransferegovPageResponse<RelatorioGestaoEspecialDTO>>() {}
        );
    }

    private <T> List<T> fetchPorPlanoAcao(
            String path,
            Long idPlanoAcao,
            ParameterizedTypeReference<TransferegovPageResponse<T>> typeReference
    ) {
        Map<String, Object> params = new LinkedHashMap<>();
        if (idPlanoAcao != null) {
            params.put("id_plano_acao", idPlanoAcao);
        }
        return fetchAllPages(path, params, typeReference);
    }

    private <T> List<T> fetchAllPages(
            String path,
            Map<String, Object> baseParams,
            ParameterizedTypeReference<TransferegovPageResponse<T>> typeReference
    ) {
        List<T> allItems = new ArrayList<>();
        int currentPage = 1;
        int totalPages = 1;

        while (currentPage <= totalPages && currentPage <= properties.maxPagesPerSync()) {
            Map<String, Object> queryParams = new LinkedHashMap<>(baseParams);
            queryParams.put("pagina", currentPage);
            queryParams.put("tamanho_da_pagina", properties.pageSize());

            TransferegovPageResponse<T> pageResponse = executeWithRetry(path, queryParams, typeReference);
            if (pageResponse == null || pageResponse.data() == null || pageResponse.data().isEmpty()) {
                break;
            }

            allItems.addAll(pageResponse.data());
            totalPages = pageResponse.totalPages() != null ? pageResponse.totalPages() : 1;
            currentPage++;

            if (currentPage <= totalPages && properties.rateLimitDelayMs() > 0) {
                try {
                    Thread.sleep(properties.rateLimitDelayMs());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        return allItems;
    }

    private <T> TransferegovPageResponse<T> executeWithRetry(
            String path,
            Map<String, Object> queryParams,
            ParameterizedTypeReference<TransferegovPageResponse<T>> typeReference
    ) {
        int maxRetries = properties.maxRetries();
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.debug("Executando requisição Transferegov especiais: {} (Tentativa {}/{})", path, attempt, maxRetries);

                return restClient.get()
                        .uri(uriBuilder -> {
                            uriBuilder.path(path.startsWith("/") ? path : "/" + path);
                            queryParams.forEach(uriBuilder::queryParam);
                            return uriBuilder.build();
                        })
                        .retrieve()
                        .body(typeReference);

            } catch (HttpStatusCodeException ex) {
                lastException = ex;
                HttpStatusCode status = ex.getStatusCode();
                log.warn("Falha HTTP {} ao acessar {}: {}", status, path, ex.getMessage());

                if (status.value() == 429 || status.is5xxServerError()) {
                    backoff(attempt);
                } else {
                    // Erros 4xx como 404/422 não devem sofrer retry
                    throw ex;
                }
            } catch (ResourceAccessException ex) {
                lastException = ex;
                log.warn("Erro de conexão I/O ao acessar {} (Tentativa {}/{}): {}", path, attempt, maxRetries, ex.getMessage());
                backoff(attempt);
            } catch (Exception ex) {
                lastException = ex;
                log.error("Erro inesperado ao acessar {}: {}", path, ex.getMessage(), ex);
                throw new RuntimeException("Erro ao comunicar com API Transferegov especiais: " + ex.getMessage(), ex);
            }
        }

        log.error("Excedido número máximo de tentativas ({}) para caminho: {}", maxRetries, path);
        if (lastException instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        throw new RuntimeException("Falha na chamada da API Transferegov após retries: " + (lastException != null ? lastException.getMessage() : "Desconhecido"), lastException);
    }

    private void backoff(int attempt) {
        long waitTimeMs = (long) Math.pow(2, attempt - 1) * 300L;
        try {
            Thread.sleep(waitTimeMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
