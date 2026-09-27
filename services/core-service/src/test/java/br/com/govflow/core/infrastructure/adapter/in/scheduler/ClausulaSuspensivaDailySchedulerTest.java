package br.com.govflow.core.infrastructure.adapter.in.scheduler;

import br.com.govflow.core.application.port.in.ConsultarClausulaSuspensivaUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClausulaSuspensivaDailySchedulerTest {

    @Mock
    private ConsultarClausulaSuspensivaUseCase consultarUseCase;

    @InjectMocks
    private ClausulaSuspensivaDailyScheduler scheduler;

    @Test
    @DisplayName("Deve disparar varredura diária de prazos preventivos de cláusula suspensiva via Use Case")
    void deveExecutarVarreduraDiaria() {
        when(consultarUseCase.executarVarreduraAlertasPrazo()).thenReturn(3);

        scheduler.executarVarreduraMatinalPrazosSuspensiva();

        verify(consultarUseCase, times(1)).executarVarreduraAlertasPrazo();
    }
}
