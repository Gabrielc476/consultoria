package br.com.govflow.transferegov.query.controller;

import br.com.govflow.transferegov.persistence.entity.SincronizacaoConvenioEntity;
import br.com.govflow.transferegov.persistence.repository.SincronizacaoConvenioRepository;
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

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Testes de Integração do Radar de Prazos (RadarPrazosController)")
class RadarPrazosControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SincronizacaoConvenioRepository convenioRepository;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    private final LocalDate dataReferencia = LocalDate.of(2026, 9, 24);

    @BeforeEach
    void setUp() {
        convenioRepository.deleteAll();

        // Convênio 1: Massaranduba - Crítico (Cláusula suspensiva vence em 5 dias)
        SincronizacaoConvenioEntity c1 = new SincronizacaoConvenioEntity();
        c1.setNrConvenio("900001/2024");
        c1.setCnpjProponente("08847784000144");
        c1.setNomeProponente("PREFEITURA DE MASSARANDUBA");
        c1.setMunicipio("Massaranduba");
        c1.setUf("PB");
        c1.setSituacaoConvenio("EM_EXECUCAO");
        c1.setInstrumentoAtivo(true);
        c1.setDataSuspensiva(dataReferencia.plusDays(5));
        c1.setDataFimVigencia(dataReferencia.plusDays(120));
        c1.setValorGlobal(new BigDecimal("500000.00"));
        c1.setValorRepasse(new BigDecimal("450000.00"));
        convenioRepository.save(c1);

        // Convênio 2: Massaranduba - Atenção (Vigência vence em 30 dias)
        SincronizacaoConvenioEntity c2 = new SincronizacaoConvenioEntity();
        c2.setNrConvenio("900002/2024");
        c2.setCnpjProponente("08847784000144");
        c2.setNomeProponente("PREFEITURA DE MASSARANDUBA");
        c2.setMunicipio("Massaranduba");
        c2.setUf("PB");
        c2.setSituacaoConvenio("EM_EXECUCAO");
        c2.setInstrumentoAtivo(true);
        c2.setDataFimVigencia(dataReferencia.plusDays(30));
        c2.setValorGlobal(new BigDecimal("300000.00"));
        c2.setValorRepasse(new BigDecimal("280000.00"));
        convenioRepository.save(c2);

        // Convênio 3: Campina Grande - Regular (Vigência vence em 100 dias)
        SincronizacaoConvenioEntity c3 = new SincronizacaoConvenioEntity();
        c3.setNrConvenio("900003/2024");
        c3.setCnpjProponente("08847784000145");
        c3.setNomeProponente("PREFEITURA DE CAMPINA GRANDE");
        c3.setMunicipio("Campina Grande");
        c3.setUf("PB");
        c3.setSituacaoConvenio("EM_EXECUCAO");
        c3.setInstrumentoAtivo(true);
        c3.setDataFimVigencia(dataReferencia.plusDays(100));
        c3.setValorGlobal(new BigDecimal("1000000.00"));
        c3.setValorRepasse(new BigDecimal("900000.00"));
        convenioRepository.save(c3);
    }

    @Test
    @DisplayName("Deve retornar o panorama consolidado com semáforo, agrupamento e alertas")
    void deveRetornarPanoramaConsolidado() throws Exception {
        mockMvc.perform(get("/api/v1/transferegov/radar-prazos")
                        .param("dataReferencia", dataReferencia.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                // Resumo de KPIs
                .andExpect(jsonPath("$.resumo.totalMonitorados", is(3)))
                .andExpect(jsonPath("$.resumo.totalCriticos", is(1)))
                .andExpect(jsonPath("$.resumo.totalAtencao", is(1)))
                .andExpect(jsonPath("$.resumo.totalRegulares", is(1)))
                .andExpect(jsonPath("$.resumo.dataReferencia", is(dataReferencia.toString())))
                // Agrupamento por município
                .andExpect(jsonPath("$.agrupamentoPorMunicipio", hasSize(2)))
                .andExpect(jsonPath("$.agrupamentoPorMunicipio[0].municipio", is("Massaranduba")))
                .andExpect(jsonPath("$.agrupamentoPorMunicipio[0].totalConvenios", is(2)))
                .andExpect(jsonPath("$.agrupamentoPorMunicipio[0].totalCriticos", is(1)))
                .andExpect(jsonPath("$.agrupamentoPorMunicipio[0].maiorRisco", is("CRITICO")))
                .andExpect(jsonPath("$.agrupamentoPorMunicipio[1].municipio", is("Campina Grande")))
                .andExpect(jsonPath("$.agrupamentoPorMunicipio[1].maiorRisco", is("REGULAR")))
                // Lista de alertas ordenada por dias restantes
                .andExpect(jsonPath("$.alertas", hasSize(3)))
                .andExpect(jsonPath("$.alertas[0].nrConvenio", is("900001/2024")))
                .andExpect(jsonPath("$.alertas[0].diasRestantes", is(5)))
                .andExpect(jsonPath("$.alertas[0].nivelRisco", is("CRITICO")))
                .andExpect(jsonPath("$.alertas[0].tipoPrazoMaisProximo", is("CLAUSULA_SUSPENSIVA")))
                .andExpect(jsonPath("$.alertas[1].nrConvenio", is("900002/2024")))
                .andExpect(jsonPath("$.alertas[1].diasRestantes", is(30)))
                .andExpect(jsonPath("$.alertas[1].nivelRisco", is("ATENCAO")));
    }

    @Test
    @DisplayName("Deve filtrar alertas por nivelRisco = CRITICO mantendo o resumo global")
    void deveFiltrarPorNivelRisco() throws Exception {
        mockMvc.perform(get("/api/v1/transferegov/radar-prazos")
                        .param("nivelRisco", "CRITICO")
                        .param("dataReferencia", dataReferencia.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumo.totalMonitorados", is(3)))
                .andExpect(jsonPath("$.resumo.totalCriticos", is(1)))
                .andExpect(jsonPath("$.alertas", hasSize(1)))
                .andExpect(jsonPath("$.alertas[0].nrConvenio", is("900001/2024")))
                .andExpect(jsonPath("$.alertas[0].nivelRisco", is("CRITICO")));
    }

    @Test
    @DisplayName("Deve filtrar alertas por município")
    void deveFiltrarPorMunicipio() throws Exception {
        mockMvc.perform(get("/api/v1/transferegov/radar-prazos")
                        .param("municipio", "Campina")
                        .param("dataReferencia", dataReferencia.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumo.totalMonitorados", is(1)))
                .andExpect(jsonPath("$.agrupamentoPorMunicipio", hasSize(1)))
                .andExpect(jsonPath("$.agrupamentoPorMunicipio[0].municipio", is("Campina Grande")))
                .andExpect(jsonPath("$.alertas", hasSize(1)))
                .andExpect(jsonPath("$.alertas[0].nrConvenio", is("900003/2024")));
    }

    @Test
    @DisplayName("Deve disparar avaliação sob demanda e emitir alertas críticos no RabbitMQ")
    void deveDispararAvaliacaoPrazosManual() throws Exception {
        mockMvc.perform(post("/api/v1/transferegov/radar-prazos/avaliar")
                        .param("dataReferencia", dataReferencia.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SUCESSO")))
                .andExpect(jsonPath("$.totalAlertasCriticosEmitidos", is(1)));

        verify(rabbitTemplate, atLeastOnce()).convertAndSend(eq("govflow.events"), eq("transferegov.prazo.alerta"), any(Object.class));
    }
}
