package br.com.govflow.core.application.port.out;

import br.com.govflow.core.domain.event.AlertaPrazoSuspensivaEvent;
import br.com.govflow.core.domain.event.ClausulaSuspensivaSuperadaEvent;

public interface ClausulaSuspensivaEventPublisherPort {

    void publicarSuperacao(ClausulaSuspensivaSuperadaEvent evento);

    void publicarAlertaPrazo(AlertaPrazoSuspensivaEvent evento);
}
