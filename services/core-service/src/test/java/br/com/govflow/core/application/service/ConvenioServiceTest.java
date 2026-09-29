package br.com.govflow.core.application.service;

import br.com.govflow.core.application.port.in.CadastrarConvenioUseCase.CadastrarConvenioCommand;
import br.com.govflow.core.application.port.out.CondicionanteSuspensivaRepositoryPort;
import br.com.govflow.core.application.port.out.ConvenioRepositoryPort;
import br.com.govflow.core.domain.exception.RegraNegocioClausulaSuspensivaException;
import br.com.govflow.core.domain.model.convenio.Convenio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConvenioServiceTest {

    @Mock
    private ConvenioRepositoryPort convenioRepository;

    @Mock
    private CondicionanteSuspensivaRepositoryPort condicionanteRepository;

    private ConvenioService convenioService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID prefeituraId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        convenioService = new ConvenioService(convenioRepository, condicionanteRepository);
    }

    @Test
    @DisplayName("Deve cadastrar convênio com cláusula suspensiva e realizar auto-seeding dos 3 pilares da Caixa GIGOV")
    void deveCadastrarConvenioComClausulaSuspensivaEAutoSeedDosTresPilares() {
        CadastrarConvenioCommand command = new CadastrarConvenioCommand(
                tenantId,
                prefeituraId,
                "954120/2026",
                "00124/2026",
                "Ministério das Cidades",
                "Pavimentação e Drenagem",
                new BigDecimal("1000000.00"),
                new BigDecimal("900000.00"),
                new BigDecimal("100000.00"),
                true,
                null,
                LocalDate.now(),
                LocalDate.now().plusYears(1)
        );

        when(convenioRepository.salvar(any(Convenio.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Convenio resultado = convenioService.cadastrar(command);

        assertThat(resultado).isNotNull();
        assertThat(resultado.isPossuiClausulaSuspensiva()).isTrue();
        assertThat(resultado.getStatusClausulaSuspensiva()).isEqualTo("PENDENTE");
        assertThat(resultado.getPrazoClausulaSuspensiva()).isEqualTo(LocalDate.now().plusDays(180));

        verify(convenioRepository, times(1)).salvar(any(Convenio.class));
        verify(condicionanteRepository, times(1)).salvarTodas(argThat(list -> list.size() == 3));
    }

    @Test
    @DisplayName("Deve cadastrar convênio sem cláusula suspensiva sem acionar auto-seeding de condicionantes")
    void deveCadastrarConvenioSemClausulaSuspensiva() {
        CadastrarConvenioCommand command = new CadastrarConvenioCommand(
                tenantId,
                prefeituraId,
                "954120/2026",
                null,
                "Ministério da Saúde",
                "Aquisição de Ambulância",
                new BigDecimal("300000.00"),
                new BigDecimal("280000.00"),
                new BigDecimal("20000.00"),
                false,
                null,
                LocalDate.now(),
                LocalDate.now().plusYears(1)
        );

        when(convenioRepository.salvar(any(Convenio.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Convenio resultado = convenioService.cadastrar(command);

        assertThat(resultado).isNotNull();
        assertThat(resultado.isPossuiClausulaSuspensiva()).isFalse();
        assertThat(resultado.getStatusClausulaSuspensiva()).isEqualTo("NAO_APLICA");

        verify(convenioRepository, times(1)).salvar(any(Convenio.class));
        verify(condicionanteRepository, never()).salvarTodas(anyList());
    }

    @Test
    @DisplayName("Deve lançar exceção se soma de repasse e contrapartida divergir do valor global")
    void deveFalharSeSomaDeValoresDivergir() {
        CadastrarConvenioCommand command = new CadastrarConvenioCommand(
                tenantId,
                prefeituraId,
                "954120/2026",
                null,
                "FNDE",
                "Construção de Creche",
                new BigDecimal("1000000.00"),
                new BigDecimal("800000.00"),
                new BigDecimal("100000.00"), // 800k + 100k != 1000k
                false,
                null,
                LocalDate.now(),
                LocalDate.now().plusYears(1)
        );

        assertThatThrownBy(() -> convenioService.cadastrar(command))
                .isInstanceOf(RegraNegocioClausulaSuspensivaException.class)
                .hasMessageContaining("Inconsistência financeira");

        verify(convenioRepository, never()).salvar(any());
    }
}
