package br.com.govflow.core.application.service;

import br.com.govflow.core.application.port.in.CadastrarPrefeituraUseCase;
import br.com.govflow.core.application.port.out.ConsultoriaRepositoryPort;
import br.com.govflow.core.application.port.out.PrefeituraRepositoryPort;
import br.com.govflow.core.domain.exception.LimitePrefeiturasExcedidoException;
import br.com.govflow.core.domain.exception.PrefeituraJaCadastradaException;
import br.com.govflow.core.domain.model.Cnpj;
import br.com.govflow.core.domain.model.Consultoria;
import br.com.govflow.core.domain.model.PlanoConsultoria;
import br.com.govflow.core.domain.model.PorteMunicipio;
import br.com.govflow.core.domain.model.Prefeitura;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrefeituraServiceTest {

    @Mock
    private PrefeituraRepositoryPort prefeituraRepository;

    @Mock
    private ConsultoriaRepositoryPort consultoriaRepository;

    @InjectMocks
    private PrefeituraService prefeituraService;

    private UUID tenantId;
    private CadastrarPrefeituraUseCase.CadastrarPrefeituraCommand command;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        command = new CadastrarPrefeituraUseCase.CadastrarPrefeituraCommand(
                tenantId,
                "08.778.326/0001-56",
                "Prefeitura Municipal de João Pessoa",
                "João Pessoa",
                "PB",
                "2507507",
                PorteMunicipio.GRANDE_PORTE,
                "Cícero Lucena",
                null,
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2028, 12, 31)
        );
    }

    @Test
    @DisplayName("Deve cadastrar prefeitura com sucesso quando dados e tenant estiverem válidos")
    void deveCadastrarPrefeituraComSucesso() {
        Consultoria consultoria = Consultoria.criarNova(
                new Cnpj("13.519.354/0001-99"),
                "SME Serviços",
                "Consultoria Um",
                "contato@sme.com.br",
                null,
                PlanoConsultoria.STARTER
        );

        when(consultoriaRepository.buscarPorId(tenantId)).thenReturn(Optional.of(consultoria));
        when(prefeituraRepository.contarPorTenantId(tenantId)).thenReturn(2L);
        when(prefeituraRepository.existePorCnpjETenantId(any(Cnpj.class), eq(tenantId))).thenReturn(false);
        when(prefeituraRepository.salvar(any(Prefeitura.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Prefeitura resultado = prefeituraService.cadastrar(command);

        assertNotNull(resultado);
        assertEquals(tenantId, resultado.getTenantId());
        assertEquals("08.778.326/0001-56", resultado.getCnpj().getFormatted());
        verify(prefeituraRepository, times(1)).salvar(any(Prefeitura.class));
    }

    @Test
    @DisplayName("Deve lançar PrefeituraJaCadastradaException quando o CNPJ já estiver cadastrado no mesmo tenant")
    void deveLancarExcecaoQuandoCnpjJaExisteNoTenant() {
        when(consultoriaRepository.buscarPorId(tenantId)).thenReturn(Optional.empty());
        when(prefeituraRepository.existePorCnpjETenantId(any(Cnpj.class), eq(tenantId))).thenReturn(true);

        assertThrows(PrefeituraJaCadastradaException.class, () -> prefeituraService.cadastrar(command));
        verify(prefeituraRepository, never()).salvar(any(Prefeitura.class));
    }

    @Test
    @DisplayName("Deve lançar LimitePrefeiturasExcedidoException quando o plano da consultoria tiver atingido a capacidade")
    void deveLancarExcecaoQuandoLimiteDoPlanoEstourar() {
        Consultoria consultoriaStarter = Consultoria.criarNova(
                new Cnpj("13.519.354/0001-99"),
                "SME Serviços",
                "Consultoria Um",
                "contato@sme.com.br",
                null,
                PlanoConsultoria.STARTER // Limite de 5 prefeituras
        );

        when(consultoriaRepository.buscarPorId(tenantId)).thenReturn(Optional.of(consultoriaStarter));
        when(prefeituraRepository.contarPorTenantId(tenantId)).thenReturn(5L); // Já atingiu 5

        assertThrows(LimitePrefeiturasExcedidoException.class, () -> prefeituraService.cadastrar(command));
        verify(prefeituraRepository, never()).salvar(any(Prefeitura.class));
    }
}
