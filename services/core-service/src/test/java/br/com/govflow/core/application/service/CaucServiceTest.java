package br.com.govflow.core.application.service;

import br.com.govflow.core.application.port.in.AtualizarCertidaoCaucUseCase.CadastrarCertidaoCommand;
import br.com.govflow.core.application.port.in.AvaliarConformidadeCaucUseCase.ResultadoAvaliacaoDto;
import br.com.govflow.core.application.port.in.ConsultarCaucUseCase.DossieCaucDto;
import br.com.govflow.core.application.port.out.CaucEventPublisherPort;
import br.com.govflow.core.application.port.out.CertidaoCaucRepositoryPort;
import br.com.govflow.core.application.port.out.PrefeituraRepositoryPort;
import br.com.govflow.core.domain.event.AlertaCertidaoCaucEvent;
import br.com.govflow.core.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CaucServiceTest {

    @Mock
    private CertidaoCaucRepositoryPort certidaoRepository;

    @Mock
    private PrefeituraRepositoryPort prefeituraRepository;

    @Mock
    private CaucEventPublisherPort eventPublisher;

    private CaucService caucService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID prefeituraId = UUID.randomUUID();
    private Prefeitura prefeitura;

    @BeforeEach
    void setUp() {
        caucService = new CaucService(certidaoRepository, prefeituraRepository, eventPublisher);

        prefeitura = new Prefeitura(
                prefeituraId,
                tenantId,
                new Cnpj("08.778.326/0001-56"),
                "Prefeitura Municipal de Teste",
                "Município Teste",
                Uf.PB,
                new CodigoIbge("2507507"),
                PorteMunicipio.MEDIO_PORTE,
                "Prefeito Teste",
                null,
                null,
                null,
                StatusCauc.ADIMPLENTE,
                true,
                java.time.Instant.now(),
                java.time.Instant.now()
        );
    }

    @Test
    @DisplayName("Deve obter dossiê e semear automaticamente as 16 certidões oficiais se banco estiver vazio")
    void deveObterDossieESincronizarDezesseisCertidoes() {
        when(prefeituraRepository.buscarPorId(prefeituraId)).thenReturn(Optional.of(prefeitura));
        when(certidaoRepository.listarPorPrefeituraId(prefeituraId)).thenReturn(new ArrayList<>());
        when(certidaoRepository.salvar(any(CertidaoCauc.class))).thenAnswer(inv -> inv.getArgument(0));

        DossieCaucDto dossie = caucService.obterDossiePrefeitura(prefeituraId);

        assertThat(dossie).isNotNull();
        assertThat(dossie.prefeituraId()).isEqualTo(prefeitura.getId());
        assertThat(dossie.certidoes()).hasSize(16);
        assertThat(dossie.certidoesRegulares()).isEqualTo(16);
        assertThat(dossie.certidoesAlerta()).isEqualTo(0);
        assertThat(dossie.certidoesVencidas()).isEqualTo(0);

        verify(certidaoRepository, times(16)).salvar(any(CertidaoCauc.class));
    }

    @Test
    @DisplayName("Deve atualizar certidão com data vencida e alterar status da prefeitura para BLOQUEADO")
    void deveAtualizarCertidaoEBloquearPrefeituraSeVencida() {
        when(prefeituraRepository.buscarPorId(prefeituraId)).thenReturn(Optional.of(prefeitura));

        CertidaoCauc certidaoVencida = new CertidaoCauc(
                UUID.randomUUID(),
                tenantId,
                prefeituraId,
                TipoExigenciaCauc.REGULARIDADE_FGTS,
                "CRF-12345",
                LocalDate.now().minusDays(90),
                LocalDate.now().minusDays(5),
                StatusCertidao.VENCIDA,
                -5,
                null,
                null,
                null
        );

        when(certidaoRepository.buscarPorPrefeituraIdETipoExigencia(prefeituraId, TipoExigenciaCauc.REGULARIDADE_FGTS))
                .thenReturn(Optional.empty());
        when(certidaoRepository.salvar(any(CertidaoCauc.class))).thenReturn(certidaoVencida);
        when(certidaoRepository.listarPorPrefeituraId(prefeituraId)).thenReturn(List.of(certidaoVencida));

        CadastrarCertidaoCommand command = new CadastrarCertidaoCommand(
                tenantId,
                prefeituraId,
                TipoExigenciaCauc.REGULARIDADE_FGTS,
                "CRF-12345",
                LocalDate.now().minusDays(90),
                LocalDate.now().minusDays(5),
                "s3://govflow/certidoes/crf.pdf",
                null
        );

        CertidaoCauc resultado = caucService.cadastrarOuAtualizarCertidao(command);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getSituacao()).isEqualTo(StatusCertidao.VENCIDA);
        assertThat(prefeitura.getStatusCauc()).isEqualTo(StatusCauc.BLOQUEADO);

        verify(prefeituraRepository).salvar(prefeitura);
        verify(eventPublisher).publicarAlerta(any(AlertaCertidaoCaucEvent.class));
    }

    @Test
    @DisplayName("Deve avaliar todas as prefeituras e disparar alertas em D-10")
    void deveAvaliarTodasPrefeiturasEDispararAlertasD10() {
        when(prefeituraRepository.listar(0, 1000, true)).thenReturn(List.of(prefeitura));

        CertidaoCauc certD10 = new CertidaoCauc(
                UUID.randomUUID(),
                tenantId,
                prefeituraId,
                TipoExigenciaCauc.RECEITA_FEDERAL_PGFN,
                "CND-001",
                LocalDate.now().minusDays(170),
                LocalDate.now().plusDays(10), // D-10
                StatusCertidao.REGULAR,
                null,
                null,
                null,
                null
        );

        when(certidaoRepository.listarPorPrefeituraId(prefeituraId)).thenReturn(new ArrayList<>(List.of(certD10)));
        when(certidaoRepository.salvar(any(CertidaoCauc.class))).thenAnswer(inv -> inv.getArgument(0));

        ResultadoAvaliacaoDto resultado = caucService.avaliarTodasPrefeituras(tenantId);

        assertThat(resultado.totalPrefeiturasAvaliadas()).isEqualTo(1);
        assertThat(resultado.totalCertidoesEmAlerta()).isGreaterThanOrEqualTo(1);
        assertThat(resultado.totalAlertasDisparados()).isGreaterThanOrEqualTo(1);

        ArgumentCaptor<AlertaCertidaoCaucEvent> eventCaptor = ArgumentCaptor.forClass(AlertaCertidaoCaucEvent.class);
        verify(eventPublisher, atLeastOnce()).publicarAlerta(eventCaptor.capture());

        AlertaCertidaoCaucEvent evento = eventCaptor.getValue();
        assertThat(evento.tipoExigencia()).isEqualTo(TipoExigenciaCauc.RECEITA_FEDERAL_PGFN.name());
        assertThat(evento.statusCertidao()).isEqualTo(StatusCertidao.ALERTA);
    }
}
