package br.com.govflow.core.infrastructure;

import br.com.govflow.core.application.port.in.CadastrarPrefeituraUseCase;
import br.com.govflow.core.application.port.out.CertidaoCaucRepositoryPort;
import br.com.govflow.core.domain.model.CertidaoCauc;
import br.com.govflow.core.domain.model.PorteMunicipio;
import br.com.govflow.core.domain.model.Prefeitura;
import br.com.govflow.core.domain.model.TipoExigenciaCauc;
import br.com.govflow.core.infrastructure.adapter.out.persistence.repository.SpringDataCertidaoCaucRepository;
import br.com.govflow.core.infrastructure.adapter.out.persistence.repository.SpringDataPrefeituraRepository;
import br.com.govflow.core.infrastructure.interceptor.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class MultiTenantCaucIsolationIntegrationTest {

    @Autowired
    private CadastrarPrefeituraUseCase cadastrarPrefeituraUseCase;

    @Autowired
    private CertidaoCaucRepositoryPort certidaoRepository;

    @Autowired
    private SpringDataCertidaoCaucRepository springDataCertidaoRepository;

    @Autowired
    private SpringDataPrefeituraRepository springDataPrefeituraRepository;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        springDataCertidaoRepository.deleteAll();
        springDataPrefeituraRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve garantir isolamento de certidões CAUC entre diferentes consultorias (Tenants)")
    void deveGarantirIsolamentoMultiTenantDeCertidoes() {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();

        // 1. Tenant A cadastra prefeitura e certidões
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

        CertidaoCauc certA1 = CertidaoCauc.criarPadrao(tenantA, prefA.getId(), TipoExigenciaCauc.RECEITA_FEDERAL_PGFN, LocalDate.now());
        CertidaoCauc certA2 = CertidaoCauc.criarPadrao(tenantA, prefA.getId(), TipoExigenciaCauc.REGULARIDADE_FGTS, LocalDate.now());

        certidaoRepository.salvar(certA1);
        certidaoRepository.salvar(certA2);

        List<CertidaoCauc> certsTenantA = certidaoRepository.listarPorPrefeituraId(prefA.getId());
        assertThat(certsTenantA).hasSize(2);

        // 2. Tenant B tenta consultar as certidões do Tenant A
        TenantContext.setCurrentTenant(tenantB);

        List<CertidaoCauc> certsTenantB = certidaoRepository.listarTodasPorTenant(tenantB);
        assertThat(certsTenantB).isEmpty();
    }
}
