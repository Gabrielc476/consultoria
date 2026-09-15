package br.com.govflow.core.infrastructure;

import br.com.govflow.core.application.port.in.CadastrarPrefeituraUseCase;
import br.com.govflow.core.application.port.in.ConsultarPrefeituraUseCase;
import br.com.govflow.core.domain.model.PorteMunicipio;
import br.com.govflow.core.domain.model.Prefeitura;
import br.com.govflow.core.infrastructure.interceptor.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

import br.com.govflow.core.infrastructure.adapter.out.persistence.repository.SpringDataPrefeituraRepository;

@SpringBootTest
@ActiveProfiles("test")
class MultiTenantIsolationIntegrationTest {

    @Autowired
    private CadastrarPrefeituraUseCase cadastrarPrefeituraUseCase;

    @Autowired
    private ConsultarPrefeituraUseCase consultarPrefeituraUseCase;

    @Autowired
    private SpringDataPrefeituraRepository repository;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        repository.deleteAll();
    }

    @Test
    @DisplayName("Critério de Aceite: Consultas do Tenant A jamais retornam dados do Tenant B (Isolamento Hibernate 6 @TenantId)")
    void deveGarantirIsolamentoTotalEntreTenantsConcorrentes() {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();

        // 1. Cadastra Prefeituras para o Tenant A (Consultoria A)
        TenantContext.setCurrentTenant(tenantA);

        Prefeitura prefA1 = cadastrarPrefeituraUseCase.cadastrar(new CadastrarPrefeituraUseCase.CadastrarPrefeituraCommand(
                tenantA,
                "08.778.326/0001-56", // João Pessoa
                "Prefeitura Municipal de João Pessoa",
                "João Pessoa",
                "PB",
                "2507507",
                PorteMunicipio.GRANDE_PORTE,
                "Cícero Lucena",
                null,
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2028, 12, 31)
        ));

        Prefeitura prefA2 = cadastrarPrefeituraUseCase.cadastrar(new CadastrarPrefeituraUseCase.CadastrarPrefeituraCommand(
                tenantA,
                "13.519.354/0001-99", // Massaranduba
                "Prefeitura Municipal de Massaranduba",
                "Massaranduba",
                "PB",
                "2509701",
                PorteMunicipio.PEQUENO_PORTE_1,
                "Paulo Oliveira",
                null,
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2028, 12, 31)
        ));

        // 2. Muda contexto para o Tenant B (Consultoria B) e cadastra Prefeitura do Tenant B
        TenantContext.setCurrentTenant(tenantB);

        Prefeitura prefB1 = cadastrarPrefeituraUseCase.cadastrar(new CadastrarPrefeituraUseCase.CadastrarPrefeituraCommand(
                tenantB,
                "08.923.456/0002-16", // Pombal
                "Prefeitura Municipal de Pombal",
                "Pombal",
                "PB",
                "2512101",
                PorteMunicipio.PEQUENO_PORTE_2,
                "Abmael de Sousa Veras",
                null,
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2028, 12, 31)
        ));

        // 3. Validação do Tenant B: deve enxergar APENAS prefB1
        List<Prefeitura> prefeiturasTenantB = consultarPrefeituraUseCase.listar(0, 10, null);
        assertEquals(1, prefeiturasTenantB.size());
        assertEquals(prefB1.getId(), prefeiturasTenantB.get(0).getId());

        // Tenant B NÃO pode achar entidades de Tenant A por ID
        Optional<Prefeitura> buscaCruzadaA1 = consultarPrefeituraUseCase.buscarPorId(prefA1.getId());
        Optional<Prefeitura> buscaCruzadaA2 = consultarPrefeituraUseCase.buscarPorId(prefA2.getId());
        assertTrue(buscaCruzadaA1.isEmpty(), "Tenant B não pode acessar Prefeitura A1 do Tenant A");
        assertTrue(buscaCruzadaA2.isEmpty(), "Tenant B não pode acessar Prefeitura A2 do Tenant A");

        // 4. Muda contexto de volta para o Tenant A
        TenantContext.setCurrentTenant(tenantA);

        List<Prefeitura> prefeiturasTenantA = consultarPrefeituraUseCase.listar(0, 10, null);
        assertEquals(2, prefeiturasTenantA.size());
        assertTrue(prefeiturasTenantA.stream().anyMatch(p -> p.getId().equals(prefA1.getId())));
        assertTrue(prefeiturasTenantA.stream().anyMatch(p -> p.getId().equals(prefA2.getId())));
        assertFalse(prefeiturasTenantA.stream().anyMatch(p -> p.getId().equals(prefB1.getId())),
                "Prefeitura do Tenant B jamais pode aparecer na listagem do Tenant A");

        // Tenant A NÃO pode achar entidade do Tenant B por ID
        Optional<Prefeitura> buscaCruzadaB1 = consultarPrefeituraUseCase.buscarPorId(prefB1.getId());
        assertTrue(buscaCruzadaB1.isEmpty(), "Tenant A não pode acessar Prefeitura B1 do Tenant B");
    }
}
