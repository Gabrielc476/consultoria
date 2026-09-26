package br.com.govflow.core.application.service;

import br.com.govflow.core.application.port.in.ConsultarClausulaSuspensivaUseCase.DossieClausulaSuspensivaDto;
import br.com.govflow.core.application.port.in.GerenciarCondicionanteUseCase.AprovarCondicionanteCommand;
import br.com.govflow.core.application.port.in.GerenciarCondicionanteUseCase.RegistrarDiligenciaCommand;
import br.com.govflow.core.application.port.out.ClausulaSuspensivaEventPublisherPort;
import br.com.govflow.core.application.port.out.ClausulaSuspensivaStoragePort;
import br.com.govflow.core.application.port.out.CondicionanteSuspensivaRepositoryPort;
import br.com.govflow.core.application.port.out.ConvenioRepositoryPort;
import br.com.govflow.core.domain.event.ClausulaSuspensivaSuperadaEvent;
import br.com.govflow.core.domain.exception.ClausulaSuspensivaNaoPodeSerSuperadaException;
import br.com.govflow.core.domain.model.convenio.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClausulaSuspensivaServiceTest {

    @Mock
    private ConvenioRepositoryPort convenioRepository;

    @Mock
    private CondicionanteSuspensivaRepositoryPort condicionanteRepository;

    @Mock
    private ClausulaSuspensivaStoragePort storagePort;

    @Mock
    private ClausulaSuspensivaEventPublisherPort eventPublisher;

    private ClausulaSuspensivaService service;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID prefeituraId = UUID.randomUUID();
    private final UUID convenioId = UUID.randomUUID();
    private Convenio convenio;

    @BeforeEach
    void setUp() {
        service = new ClausulaSuspensivaService(convenioRepository, condicionanteRepository, storagePort, eventPublisher);
        ReflectionTestUtils.setField(service, "bucketDocumentos", "govflow-documentos");

        convenio = new Convenio(
                convenioId,
                tenantId,
                prefeituraId,
                "914250/2023",
                "00124/2023",
                "FNDE / MEC",
                "Construção de Creche Proinfância Tipo 2",
                new BigDecimal("2050000.00"),
                new BigDecimal("1850000.00"),
                new BigDecimal("200000.00"),
                "EM_EXECUCAO",
                true,
                LocalDate.now().plusDays(100),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2027, 1, 1),
                false,
                null,
                null,
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    @DisplayName("Deve montar dossiê da cláusula suspensiva executando auto-seeding dos três pilares")
    void deveMontarDossieComAutoSeeding() {
        when(convenioRepository.buscarPorId(convenioId)).thenReturn(Optional.of(convenio));
        when(condicionanteRepository.buscarPorConvenioId(convenioId)).thenReturn(new ArrayList<>());
        when(condicionanteRepository.salvarTodas(any())).thenAnswer(invocation -> invocation.getArgument(0));

        DossieClausulaSuspensivaDto dossie = service.obterDossiePorConvenioId(convenioId);

        assertThat(dossie).isNotNull();
        assertThat(dossie.convenioId()).isEqualTo(convenioId);
        assertThat(dossie.numeroSiconv()).isEqualTo("914250/2023");
        assertThat(dossie.condicionantes()).hasSize(3);
        assertThat(dossie.superada()).isFalse();

        verify(condicionanteRepository).salvarTodas(any());
    }

    @Test
    @DisplayName("Deve submeter condicionante para análise da Caixa")
    void deveSubmeterParaAnaliseCaixa() {
        when(convenioRepository.buscarPorId(convenioId)).thenReturn(Optional.of(convenio));
        var cond = CondicionanteSuspensiva.nova(tenantId, convenioId, TipoCondicionanteSuspensiva.ENGENHARIA_PROJETOS_SINAPI);
        when(condicionanteRepository.buscarPorConvenioETipo(convenioId, TipoCondicionanteSuspensiva.ENGENHARIA_PROJETOS_SINAPI))
                .thenReturn(Optional.of(cond));
        when(condicionanteRepository.salvar(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CondicionanteSuspensiva resultado = service.submeterParaAnaliseCaixa(convenioId, TipoCondicionanteSuspensiva.ENGENHARIA_PROJETOS_SINAPI);

        assertThat(resultado.getStatus()).isEqualTo(StatusCondicionanteSuspensiva.EM_ANALISE_CAIXA);
        verify(condicionanteRepository).salvar(cond);
    }

    @Test
    @DisplayName("Deve registrar diligência emitida pela Caixa")
    void deveRegistrarDiligencia() {
        when(convenioRepository.buscarPorId(convenioId)).thenReturn(Optional.of(convenio));
        var cond = CondicionanteSuspensiva.nova(tenantId, convenioId, TipoCondicionanteSuspensiva.LICENCIAMENTO_AMBIENTAL);
        when(condicionanteRepository.buscarPorConvenioETipo(convenioId, TipoCondicionanteSuspensiva.LICENCIAMENTO_AMBIENTAL))
                .thenReturn(Optional.of(cond));
        when(condicionanteRepository.salvar(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDate dataSaneamento = LocalDate.now().plusDays(20);
        var cmd = new RegistrarDiligenciaCommand(
                convenioId,
                TipoCondicionanteSuspensiva.LICENCIAMENTO_AMBIENTAL,
                "Apresentar Outorga de Recursos Hídricos",
                "s3/laudo_amb.pdf",
                dataSaneamento
        );

        CondicionanteSuspensiva resultado = service.registrarDiligenciaCaixa(cmd);

        assertThat(resultado.getStatus()).isEqualTo(StatusCondicionanteSuspensiva.DILIGENCIA_EMITIDA);
        assertThat(resultado.getObservacoesAnaliseCaixa()).isEqualTo("Apresentar Outorga de Recursos Hídricos");
        assertThat(resultado.getDataLimiteSaneamento()).isEqualTo(dataSaneamento);
        verify(condicionanteRepository).salvar(cond);
    }

    @Test
    @DisplayName("Deve aprovar condicionante com documentação técnica")
    void deveAprovarCondicionante() {
        when(convenioRepository.buscarPorId(convenioId)).thenReturn(Optional.of(convenio));
        var cond = CondicionanteSuspensiva.nova(tenantId, convenioId, TipoCondicionanteSuspensiva.ENGENHARIA_PROJETOS_SINAPI);
        when(condicionanteRepository.buscarPorConvenioETipo(convenioId, TipoCondicionanteSuspensiva.ENGENHARIA_PROJETOS_SINAPI))
                .thenReturn(Optional.of(cond));
        when(condicionanteRepository.salvar(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var cmd = new AprovarCondicionanteCommand(
                convenioId,
                TipoCondicionanteSuspensiva.ENGENHARIA_PROJETOS_SINAPI,
                "SPA-914250/2026",
                LocalDate.now(),
                null,
                new BigDecimal("2050000.00"),
                new BigDecimal("22.50"),
                "ART-PB-12345",
                "Caixa GIGOV",
                "s3/lae_spa.pdf"
        );

        CondicionanteSuspensiva resultado = service.aprovarCondicionante(cmd);

        assertThat(resultado.isAprovado()).isTrue();
        assertThat(resultado.getNumeroDocumentoComprobatorio()).isEqualTo("SPA-914250/2026");
        assertThat(resultado.getValorOrcamentoAprovadoCaixa()).isEqualByComparingTo(new BigDecimal("2050000.00"));
        verify(condicionanteRepository).salvar(cond);
    }

    @Test
    @DisplayName("Deve solicitar prorrogação de prazo fatal da cláusula suspensiva")
    void deveSolicitarProrrogacao() {
        when(convenioRepository.buscarPorId(convenioId)).thenReturn(Optional.of(convenio));
        when(convenioRepository.salvar(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDate novoPrazo = convenio.getPrazoClausulaSuspensiva().plusDays(60);
        Convenio resultado = service.solicitarProrrogacao(convenioId, novoPrazo);

        assertThat(resultado.isProrrogacaoSolicitada()).isTrue();
        assertThat(resultado.getNovoPrazoProrrogado()).isEqualTo(novoPrazo);
        verify(convenioRepository).salvar(convenio);
    }

    @Test
    @DisplayName("Deve superar Cláusula Suspensiva e disparar evento RabbitMQ quando todos os 3 pilares estiverem aprovados")
    void deveSuperarClausulaSuspensivaComSucesso() {
        when(convenioRepository.buscarPorId(convenioId)).thenReturn(Optional.of(convenio));
        when(convenioRepository.salvar(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var condEng = CondicionanteSuspensiva.nova(tenantId, convenioId, TipoCondicionanteSuspensiva.ENGENHARIA_PROJETOS_SINAPI);
        condEng.aprovar("SPA-01", LocalDate.now(), null, new BigDecimal("2000000"), new BigDecimal("22"), "ART-01", "GIGOV", "s3/eng.pdf");

        var condAmb = CondicionanteSuspensiva.nova(tenantId, convenioId, TipoCondicionanteSuspensiva.LICENCIAMENTO_AMBIENTAL);
        condAmb.aprovar("LI-01", LocalDate.now(), LocalDate.now().plusYears(1), null, null, null, "SUDEMA", "s3/amb.pdf");

        var condTit = CondicionanteSuspensiva.nova(tenantId, convenioId, TipoCondicionanteSuspensiva.TITULARIDADE_IMOVEL);
        condTit.aprovar("CRI-01", LocalDate.now(), LocalDate.now().plusDays(60), null, null, null, "CRI Patos", "s3/cri.pdf");

        when(condicionanteRepository.buscarPorConvenioId(convenioId)).thenReturn(List.of(condEng, condAmb, condTit));

        Convenio resultado = service.superarClausulaSuspensiva(convenioId, "s3/termo_retirada.pdf");

        assertThat(resultado.isClausulaSuspensivaSuperada()).isTrue();
        assertThat(resultado.getS3KeyTermoRetiradaSuspensiva()).isEqualTo("s3/termo_retirada.pdf");

        ArgumentCaptor<ClausulaSuspensivaSuperadaEvent> captor = ArgumentCaptor.forClass(ClausulaSuspensivaSuperadaEvent.class);
        verify(eventPublisher).publicarSuperacao(captor.capture());
        assertThat(captor.getValue().convenioId()).isEqualTo(convenioId);
        assertThat(captor.getValue().numeroSiconv()).isEqualTo("914250/2023");
    }

    @Test
    @DisplayName("Deve bloquear superação se houver pilar pendente de aprovação")
    void deveBloquearSuperacaoComPilaresPendentes() {
        when(convenioRepository.buscarPorId(convenioId)).thenReturn(Optional.of(convenio));

        var condEng = CondicionanteSuspensiva.nova(tenantId, convenioId, TipoCondicionanteSuspensiva.ENGENHARIA_PROJETOS_SINAPI);
        condEng.aprovar("SPA-01", LocalDate.now(), null, new BigDecimal("2000000"), new BigDecimal("22"), "ART-01", "GIGOV", "s3/eng.pdf");

        var condAmb = CondicionanteSuspensiva.nova(tenantId, convenioId, TipoCondicionanteSuspensiva.LICENCIAMENTO_AMBIENTAL);
        // Ambiente não aprovado

        var condTit = CondicionanteSuspensiva.nova(tenantId, convenioId, TipoCondicionanteSuspensiva.TITULARIDADE_IMOVEL);
        condTit.aprovar("CRI-01", LocalDate.now(), LocalDate.now().plusDays(60), null, null, null, "CRI Patos", "s3/cri.pdf");

        when(condicionanteRepository.buscarPorConvenioId(convenioId)).thenReturn(List.of(condEng, condAmb, condTit));

        assertThatThrownBy(() -> service.superarClausulaSuspensiva(convenioId, "s3/termo.pdf"))
                .isInstanceOf(ClausulaSuspensivaNaoPodeSerSuperadaException.class);

        verify(eventPublisher, never()).publicarSuperacao(any());
    }

    @Test
    @DisplayName("Deve fazer upload de documento e vincular na condicionante")
    void deveFazerUploadDeDocumento() {
        when(convenioRepository.buscarPorId(convenioId)).thenReturn(Optional.of(convenio));
        var cond = CondicionanteSuspensiva.nova(tenantId, convenioId, TipoCondicionanteSuspensiva.TITULARIDADE_IMOVEL);
        when(condicionanteRepository.buscarPorConvenioETipo(convenioId, TipoCondicionanteSuspensiva.TITULARIDADE_IMOVEL))
                .thenReturn(Optional.of(cond));
        when(condicionanteRepository.salvar(any())).thenAnswer(invocation -> invocation.getArgument(0));

        byte[] bytes = "conteudo certidao cri".getBytes();
        CondicionanteSuspensiva resultado = service.uploadDocumentoComprobatorio(
                convenioId,
                TipoCondicionanteSuspensiva.TITULARIDADE_IMOVEL,
                "matricula_cri.pdf",
                "application/pdf",
                bytes
        );

        verify(storagePort).salvarArquivo(eq("govflow-documentos"), any(), eq(bytes), eq("application/pdf"));
        assertThat(resultado.getS3KeyDocumento()).contains("clausula-suspensiva");
        assertThat(resultado.getS3KeyDocumento()).contains("matricula_cri.pdf");
    }
}
