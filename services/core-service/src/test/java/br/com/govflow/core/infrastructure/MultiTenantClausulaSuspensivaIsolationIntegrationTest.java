package br.com.govflow.core.infrastructure;

import br.com.govflow.core.application.port.in.CadastrarPrefeituraUseCase;
import br.com.govflow.core.application.port.out.CondicionanteSuspensivaRepositoryPort;
import br.com.govflow.core.application.port.out.ConvenioRepositoryPort;
import br.com.govflow.core.domain.model.PorteMunicipio;
import br.com.govflow.core.domain.model.Prefeitura;
import br.com.govflow.core.domain.model.convenio.CondicionanteSuspensiva;
import br.com.govflow.core.domain.model.convenio.Convenio;
import br.com.govflow.core.domain.model.convenio.TipoCondicionanteSuspensiva;
import br.com.govflow.core.infrastructure.adapter.out.persistence.repository.SpringDataCondicionanteSuspensivaRepository;
import br.com.govflow.core.infrastructure.adapter.out.persistence.repository.SpringDataConvenioRepository;
import br.com.govflow.core.infrastructure.adapter.out.persistence.repository.SpringDataPrefeituraRepository;
import br.com.govflow.core.infrastructure.interceptor.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class MultiTenantClausulaSuspensivaIsolationIntegrationTest {

    @Autowired
    private CadastrarPrefeituraUseCase cadastrarPrefeituraUseCase;

    @Autowired
    private ConvenioRepositoryPort convenioRepository;

    @Autowired
    private CondicionanteSuspensivaRepositoryPort condicionanteRepository;

    @Autowired
    private SpringDataCondicionanteSuspensivaRepository springDataCondicionanteRepository;

    @Autowired
    private SpringDataConvenioRepository springDataConvenioRepository;

    @Autowired
    private SpringDataPrefeituraRepository springDataPrefeituraRepository;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        springDataCondicionanteRepository.deleteAll();
        springDataConvenioRepository.deleteAll();
        springDataPrefeituraRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve garantir isolamento multi-tenant de convênios e condicionantes suspensivas entre diferentes consultorias")
    void deveGarantirIsolamentoMultiTenantDeConveniosECondicionantes() {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();

        // 1. Tenant A cadastra prefeitura, convênio e condicionantes
        TenantContext.setCurrentTenant(tenantA);

        Prefeitura prefA = cadastrarPrefeituraUseCase.cadastrar(new CadastrarPrefeituraUseCase.CadastrarPrefeituraCommand(
                tenantA,
                "08.778.326/0001-56",
                "Prefeitura Municipal de Patos",
                "Patos",
                "PB",
                "2510808",
                PorteMunicipio.MEDIO_PORTE,
                "Prefeito A",
                null,
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2028, 12, 31)
        ));

        Convenio convA = new Convenio(
                UUID.randomUUID(),
                tenantA,
                prefA.getId(),
                "914250/2023",
                "001/2023",
                "FNDE / MEC",
                "Construção de Creche Proinfância",
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
        convenioRepository.salvar(convA);

        CondicionanteSuspensiva condA = CondicionanteSuspensiva.nova(tenantA, convA.getId(), TipoCondicionanteSuspensiva.ENGENHARIA_PROJETOS_SINAPI);
        condA.aprovar("SPA-01", LocalDate.now(), null, new BigDecimal("2050000"), new BigDecimal("22.12"), "ART-01", "GIGOV", "s3/spa.pdf");
        condicionanteRepository.salvar(condA);

        // Validação no contexto do Tenant A
        List<Convenio> conveniosA = convenioRepository.listarPorPrefeitura(prefA.getId());
        assertThat(conveniosA).hasSize(1);
        assertThat(conveniosA.get(0).getNumeroSiconv()).isEqualTo("914250/2023");

        List<CondicionanteSuspensiva> condsA = condicionanteRepository.buscarPorConvenioId(convA.getId());
        assertThat(condsA).hasSize(1);
        assertThat(condsA.get(0).getNumeroDocumentoComprobatorio()).isEqualTo("SPA-01");

        // 2. Mudança de contexto para Tenant B
        TenantContext.setCurrentTenant(tenantB);

        // Tenant B não pode enxergar convênios ou condicionantes do Tenant A
        List<Convenio> conveniosB = convenioRepository.listarPorPrefeitura(prefA.getId());
        assertThat(conveniosB).isEmpty();

        List<CondicionanteSuspensiva> condsB = condicionanteRepository.buscarPorConvenioId(convA.getId());
        assertThat(condsB).isEmpty();

        assertThat(convenioRepository.buscarPorNumeroSiconv("914250/2023")).isEmpty();
    }
}
