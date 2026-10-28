package br.com.govflow.core.infrastructure;

import br.com.govflow.core.application.port.in.CadastrarPrefeituraUseCase;
import br.com.govflow.core.application.port.out.ConvenioRepositoryPort;
import br.com.govflow.core.application.port.out.LicitacaoRepositoryPort;
import br.com.govflow.core.domain.model.PorteMunicipio;
import br.com.govflow.core.domain.model.Prefeitura;
import br.com.govflow.core.domain.model.convenio.Convenio;
import br.com.govflow.core.domain.model.licitacao.CriterioJulgamento;
import br.com.govflow.core.domain.model.licitacao.Licitacao;
import br.com.govflow.core.domain.model.licitacao.ModalidadeLicitacao;
import br.com.govflow.core.domain.model.licitacao.RegimeExecucao;
import br.com.govflow.core.infrastructure.adapter.out.persistence.repository.SpringDataConvenioRepository;
import br.com.govflow.core.infrastructure.adapter.out.persistence.repository.SpringDataLicitacaoRepository;
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
class MultiTenantLicitacaoIsolationIntegrationTest {

    @Autowired
    private CadastrarPrefeituraUseCase cadastrarPrefeituraUseCase;

    @Autowired
    private ConvenioRepositoryPort convenioRepository;

    @Autowired
    private LicitacaoRepositoryPort licitacaoRepository;

    @Autowired
    private SpringDataLicitacaoRepository springDataLicitacaoRepository;

    @Autowired
    private SpringDataConvenioRepository springDataConvenioRepository;

    @Autowired
    private SpringDataPrefeituraRepository springDataPrefeituraRepository;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        springDataLicitacaoRepository.deleteAll();
        springDataConvenioRepository.deleteAll();
        springDataPrefeituraRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve isolar licitações entre tenants via Hibernate @TenantId")
    void deveGarantirIsolamentoMultiTenantDeLicitacoes() {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();

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

        Licitacao licitacaoA = Licitacao.cadastrar(
                tenantA,
                convA.getId(),
                prefA.getId(),
                "001/2026",
                "Concorrência 002/2026",
                ModalidadeLicitacao.CONCORRENCIA_ELETRONICA,
                CriterioJulgamento.MENOR_PRECO,
                RegimeExecucao.EMPREITADA_PRECO_UNITARIO,
                "Obras civis",
                new BigDecimal("2000000.00"),
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 20),
                null, null, null
        );
        licitacaoRepository.salvar(licitacaoA);

        List<Licitacao> doTenantA = licitacaoRepository.listarPorConvenioId(convA.getId());
        assertThat(doTenantA).hasSize(1);
        assertThat(doTenantA.get(0).getNumeroLicitacao()).isEqualTo("Concorrência 002/2026");

        TenantContext.setCurrentTenant(tenantB);

        assertThat(licitacaoRepository.listarPorConvenioId(convA.getId())).isEmpty();
        assertThat(convenioRepository.buscarPorNumeroSiconv("914250/2023")).isEmpty();
    }
}
