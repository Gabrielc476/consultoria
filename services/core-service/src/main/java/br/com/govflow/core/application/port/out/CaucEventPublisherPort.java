package br.com.govflow.core.application.port.out;

import br.com.govflow.core.domain.event.AlertaCertidaoCaucEvent;

public interface CaucEventPublisherPort {

    void publicarAlerta(AlertaCertidaoCaucEvent evento);
}
