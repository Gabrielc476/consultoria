package br.com.govflow.transferegov.event.producer;

import br.com.govflow.transferegov.domain.radar.AlertaConvenioDTO;
import br.com.govflow.transferegov.domain.radar.NivelRisco;
import br.com.govflow.transferegov.domain.radar.TipoPrazo;
import br.com.govflow.transferegov.event.model.AlertaPrazoExpirandoEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes Unitários do Publicador de Eventos RabbitMQ (TransferegovEventPublisher)")
class TransferegovEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private TransferegovEventPublisher publisher;
    private final String exchange = "govflow.events";
    private final String routingKey = "transferegov.prazo.alerta";

    @BeforeEach
    void setUp() {
        publisher = new TransferegovEventPublisher(rabbitTemplate, exchange, routingKey);
    }

    @Test
    @DisplayName("Deve publicar evento quando convênio for CRITICO")
    void devePublicarEventoQuandoCritico() {
        AlertaConvenioDTO alertaCritico = new AlertaConvenioDTO(
                UUID.randomUUID(),
                "912345/2024",
                "054321/2024",
                "Massaranduba",
                "PB",
                "08847784000144",
                "PREFEITURA MUNICIPAL DE MASSARANDUBA",
                "Pavimentação de Ruas",
                "EM_EXECUCAO",
                NivelRisco.CRITICO,
                TipoPrazo.CLAUSULA_SUSPENSIVA,
                LocalDate.now().plusDays(5),
                5L,
                LocalDate.now().plusDays(100),
                100L,
                LocalDate.now().plusDays(5),
                5L,
                LocalDate.now().plusDays(160),
                160L,
                new BigDecimal("500000.00"),
                new BigDecimal("450000.00")
        );

        publisher.publicarAlertaPrazoSeCritico(alertaCritico);

        ArgumentCaptor<AlertaPrazoExpirandoEvent> captor = ArgumentCaptor.forClass(AlertaPrazoExpirandoEvent.class);
        verify(rabbitTemplate, times(1)).convertAndSend(eq(exchange), eq(routingKey), captor.capture());

        AlertaPrazoExpirandoEvent eventCapturado = captor.getValue();
        assertThat(eventCapturado.nrConvenio()).isEqualTo("912345/2024");
        assertThat(eventCapturado.nivelRisco()).isEqualTo(NivelRisco.CRITICO);
        assertThat(eventCapturado.tipoPrazo()).isEqualTo(TipoPrazo.CLAUSULA_SUSPENSIVA);
        assertThat(eventCapturado.diasRestantes()).isEqualTo(5L);
        assertThat(eventCapturado.municipio()).isEqualTo("Massaranduba");
    }

    @Test
    @DisplayName("Não deve publicar evento quando convênio for ATENCAO ou REGULAR")
    void naoDevePublicarQuandoNaoForCritico() {
        AlertaConvenioDTO alertaAtencao = new AlertaConvenioDTO(
                UUID.randomUUID(),
                "912346/2024",
                "054322/2024",
                "Campina Grande",
                "PB",
                "08847784000145",
                "PREFEITURA MUNICIPAL DE CAMPINA GRANDE",
                "Reforma de Creche",
                "EM_EXECUCAO",
                NivelRisco.ATENCAO,
                TipoPrazo.FIM_VIGENCIA,
                LocalDate.now().plusDays(30),
                30L,
                LocalDate.now().plusDays(30),
                30L,
                null,
                null,
                null,
                null,
                new BigDecimal("300000.00"),
                new BigDecimal("280000.00")
        );

        publisher.publicarAlertaPrazoSeCritico(alertaAtencao);

        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    @DisplayName("Deve publicar alerta de inconformidade ADPF 854 na exchange com routing key específica")
    void devePublicarAlertaInconformidadeAdpf854() {
        var event = br.com.govflow.transferegov.event.model.AlertaInconformidadeAdpf854Event.of(
                999L,
                "2024.999",
                "PREFEITURA DE SOUSA",
                "PB",
                "08847784000144",
                "DEPUTADO FEDERAL",
                2024,
                new BigDecimal("500000.00"),
                br.com.govflow.transferegov.domain.compliance.StatusAdpf854.NAO_CONFORME,
                "AUSENCIA_PLANO_TRABALHO",
                "CRITICO",
                "Ausência de Plano de Trabalho cadastrado no Transferegov"
        );

        publisher.publicarAlertaInconformidadeAdpf854(event);

        verify(rabbitTemplate, times(1)).convertAndSend(eq("govflow.events"), eq("transferegov.emenda.adpf854.alerta"), eq(event));
    }
}
