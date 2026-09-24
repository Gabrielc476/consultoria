package br.com.govflow.transferegov.sync.client;

import br.com.govflow.transferegov.config.SiconvProperties;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;

/**
 * Implementação que provê os CNPJs monitorados configurados nas propriedades da aplicação.
 */
@Component
public class ConfigurationMonitoredCnpjProvider implements MonitoredCnpjProvider {

    private final SiconvProperties properties;

    public ConfigurationMonitoredCnpjProvider(SiconvProperties properties) {
        this.properties = properties;
    }

    @Override
    public Set<String> getMonitoredCnpjs() {
        return properties.targetCnpjs() != null ? properties.targetCnpjs() : Collections.emptySet();
    }
}
