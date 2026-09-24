package br.com.govflow.transferegov.sync.client;

import java.util.Set;

/**
 * Provedor de CNPJs de prefeituras/municípios monitorados para filtragem otimizada no pipeline ETL.
 */
public interface MonitoredCnpjProvider {

    /**
     * Retorna o conjunto de CNPJs (somente números ou formatados) monitorados para sincronização.
     */
    Set<String> getMonitoredCnpjs();
}
