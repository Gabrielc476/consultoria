package br.com.govflow.transferegov.query.controller;

import br.com.govflow.transferegov.domain.compliance.StatusAdpf854;
import br.com.govflow.transferegov.persistence.entity.EmendaEspecialInconformidadeEntity;
import br.com.govflow.transferegov.persistence.entity.EmendaEspecialPlanoAcaoEntity;
import br.com.govflow.transferegov.persistence.entity.EmendaEspecialPlanoTrabalhoEntity;
import br.com.govflow.transferegov.persistence.entity.EmendaEspecialRelatorioGestaoEntity;
import br.com.govflow.transferegov.persistence.repository.EmendaEspecialInconformidadeRepository;
import br.com.govflow.transferegov.persistence.repository.EmendaEspecialPlanoAcaoRepository;
import br.com.govflow.transferegov.persistence.repository.EmendaEspecialPlanoTrabalhoRepository;
import br.com.govflow.transferegov.persistence.repository.EmendaEspecialRelatorioGestaoRepository;
import br.com.govflow.transferegov.sync.client.TransferegovEspeciaisClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Testes de Integração dos Endpoints de Emendas Especiais e Auditoria ADPF 854")
class EmendaEspecialQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmendaEspecialPlanoAcaoRepository planoAcaoRepository;

    @Autowired
    private EmendaEspecialPlanoTrabalhoRepository planoTrabalhoRepository;

    @Autowired
    private EmendaEspecialRelatorioGestaoRepository relatorioGestaoRepository;

    @Autowired
    private EmendaEspecialInconformidadeRepository inconformidadeRepository;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @MockBean
    private TransferegovEspeciaisClient especiaisClient;

    @BeforeEach
    void setUp() {
        inconformidadeRepository.deleteAll();
        relatorioGestaoRepository.deleteAll();
        planoTrabalhoRepository.deleteAll();
        planoAcaoRepository.deleteAll();

        // 1. Emenda Conforme (Patos)
        EmendaEspecialPlanoAcaoEntity e1 = new EmendaEspecialPlanoAcaoEntity();
        e1.setIdPlanoAcao(8001L);
        e1.setCodigoPlanoAcao("2024.PATOS01");
        e1.setAnoPlanoAcao(2024);
        e1.setSituacaoPlanoAcao("CIENTE");
        e1.setNomeBeneficiario("PREFEITURA DE PATOS");
        e1.setCnpjBeneficiario("09123456000188");
        e1.setUfBeneficiario("PB");
        e1.setNomeParlamentar("DEPUTADO SILVA");
        e1.setAnoEmenda(2024);
        e1.setNumeroEmenda(1001);
        e1.setValorTotal(new BigDecimal("500000.00"));
        e1.setValorInvestimento(new BigDecimal("500000.00"));
        e1.setNomeObjeto("Construção de Praça Pública");
        e1.setCodigoBanco("001");
        e1.setNomeBanco("BANCO DO BRASIL");
        e1.setNumeroAgencia("1234");
        e1.setNumeroConta("56789");
        e1.setStatusAdpf854(StatusAdpf854.CONFORME);
        e1 = planoAcaoRepository.save(e1);

        EmendaEspecialPlanoTrabalhoEntity pt1 = new EmendaEspecialPlanoTrabalhoEntity();
        pt1.setIdPlanoTrabalho(9101L);
        pt1.setPlanoAcao(e1);
        pt1.setIdPlanoAcao(8001L);
        pt1.setSituacaoPlanoTrabalho("APROVADO");
        pt1.setDataInicioExecucao(LocalDate.of(2024, 1, 1));
        pt1.setDataFimExecucao(LocalDate.of(2025, 1, 1));
        planoTrabalhoRepository.save(pt1);

        EmendaEspecialRelatorioGestaoEntity rg1 = new EmendaEspecialRelatorioGestaoEntity();
        rg1.setIdRelatorioGestaoNovo(9201L);
        rg1.setPlanoAcao(e1);
        rg1.setIdPlanoAcao(8001L);
        rg1.setTipoRelatorio("Final");
        rg1.setSituacaoRelatorio("DISPONIBILIZADO");
        rg1.setValorExecutado(new BigDecimal("500000.00"));
        relatorioGestaoRepository.save(rg1);

        // 2. Emenda Não Conforme (Sousa - Ausência de plano de trabalho)
        EmendaEspecialPlanoAcaoEntity e2 = new EmendaEspecialPlanoAcaoEntity();
        e2.setIdPlanoAcao(8002L);
        e2.setCodigoPlanoAcao("2024.SOUSA01");
        e2.setAnoPlanoAcao(2024);
        e2.setSituacaoPlanoAcao("CIENTE");
        e2.setNomeBeneficiario("PREFEITURA DE SOUSA");
        e2.setCnpjBeneficiario("08847784000144");
        e2.setUfBeneficiario("PB");
        e2.setNomeParlamentar("DEPUTADO SOUZA");
        e2.setAnoEmenda(2024);
        e2.setNumeroEmenda(1002);
        e2.setValorTotal(new BigDecimal("300000.00"));
        e2.setValorCusteio(new BigDecimal("300000.00"));
        e2.setNomeObjeto("Custeio SUS");
        e2.setStatusAdpf854(StatusAdpf854.NAO_CONFORME);
        e2 = planoAcaoRepository.save(e2);

        EmendaEspecialInconformidadeEntity inconf = new EmendaEspecialInconformidadeEntity(
                e2,
                br.com.govflow.transferegov.domain.compliance.TipoInconformidadeAdpf854.AUSENCIA_PLANO_TRABALHO,
                br.com.govflow.transferegov.domain.compliance.SeveridadeInconformidade.CRITICO,
                "Ausência de Plano de Trabalho cadastrado no Transferegov para aplicação dos recursos (STF ADPF 854)"
        );
        inconformidadeRepository.save(inconf);
    }

    @Test
    @DisplayName("GET /api/v1/transferegov/emendas-especiais - Deve listar emendas com paginação")
    void deveListarEmendas() throws Exception {
        mockMvc.perform(get("/api/v1/transferegov/emendas-especiais")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements", is(2)));
    }

    @Test
    @DisplayName("GET /api/v1/transferegov/emendas-especiais com filtro de status - Deve retornar apenas não conformes")
    void deveFiltrarPorStatusNaoConforme() throws Exception {
        mockMvc.perform(get("/api/v1/transferegov/emendas-especiais")
                        .param("statusAdpf854", "NAO_CONFORME")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].codigoPlanoAcao", is("2024.SOUSA01")))
                .andExpect(jsonPath("$.content[0].statusAdpf854", is("NAO_CONFORME")));
    }

    @Test
    @DisplayName("GET /api/v1/transferegov/emendas-especiais/{idOuCodigo} - Deve retornar detalhe da emenda por ID federal")
    void deveBuscarDetalhePorIdFederal() throws Exception {
        mockMvc.perform(get("/api/v1/transferegov/emendas-especiais/8001")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idPlanoAcao", is(8001)))
                .andExpect(jsonPath("$.codigoPlanoAcao", is("2024.PATOS01")))
                .andExpect(jsonPath("$.nomeBeneficiario", is("PREFEITURA DE PATOS")))
                .andExpect(jsonPath("$.statusAdpf854", is("CONFORME")))
                .andExpect(jsonPath("$.planosTrabalho", hasSize(1)))
                .andExpect(jsonPath("$.relatoriosGestao", hasSize(1)));
    }

    @Test
    @DisplayName("GET /api/v1/transferegov/emendas-especiais/auditoria-adpf854 - Deve retornar painel executivo com métricas e alertas")
    void deveRetornarPainelAuditoria() throws Exception {
        mockMvc.perform(get("/api/v1/transferegov/emendas-especiais/auditoria-adpf854")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEmendasMonitoradas", is(2)))
                .andExpect(jsonPath("$.valorTotalMonitorado", is(800000.00)))
                .andExpect(jsonPath("$.valorTotalEmRisco", is(300000.00)))
                .andExpect(jsonPath("$.totalConforme", is(1)))
                .andExpect(jsonPath("$.totalNaoConforme", is(1)))
                .andExpect(jsonPath("$.percentualConformidade", is(50.0)))
                .andExpect(jsonPath("$.resumoMunicipios", hasSize(2)))
                .andExpect(jsonPath("$.alertasCriticos", hasSize(1)))
                .andExpect(jsonPath("$.alertasCriticos[0].tipoInconformidade", is("AUSENCIA_PLANO_TRABALHO")));
    }
}
