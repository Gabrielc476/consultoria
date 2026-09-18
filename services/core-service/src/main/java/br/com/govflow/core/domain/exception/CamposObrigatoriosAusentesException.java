package br.com.govflow.core.domain.exception;

import java.util.List;

public class CamposObrigatoriosAusentesException extends DomainException {

    private final List<String> camposFaltantes;

    public CamposObrigatoriosAusentesException(List<String> camposFaltantes) {
        super("CAMPOS_OBRIGATORIOS_AUSENTES",
                "Aprovação rejeitada. Os seguintes campos obrigatórios não foram preenchidos: " + String.join(", ", camposFaltantes));
        this.camposFaltantes = camposFaltantes != null ? List.copyOf(camposFaltantes) : List.of();
    }

    public List<String> getCamposFaltantes() {
        return camposFaltantes;
    }
}
