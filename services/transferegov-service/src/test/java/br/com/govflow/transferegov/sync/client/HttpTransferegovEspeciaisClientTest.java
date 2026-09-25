package br.com.govflow.transferegov.sync.client;

import br.com.govflow.transferegov.config.EspeciaisProperties;
import br.com.govflow.transferegov.sync.client.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@DisplayName("Testes Unitários do Cliente REST Federal (HttpTransferegovEspeciaisClient)")
class HttpTransferegovEspeciaisClientTest {

    private MockRestServiceServer mockServer;
    private HttpTransferegovEspeciaisClient client;
    private EspeciaisProperties properties;

    @BeforeEach
    void setUp() {
        properties = new EspeciaisProperties(
                "http://localhost:8089/especiais",
                1000,
                2000,
                2,
                10,
                "PB",
                0L,
                3,
                "-",
                Collections.emptySet()
        );

        RestClient.Builder builder = RestClient.builder().baseUrl(properties.baseUrl());
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        client = new HttpTransferegovEspeciaisClient(restClient, properties);
    }

    @Test
    @DisplayName("Deve consultar beneficiários percorrendo múltiplas páginas automaticamente")
    void deveConsultarBeneficiariosComPaginacao() {
        String page1Json = """
                {
                    "data": [
                        {
                            "id_beneficiario": 101,
                            "uf_beneficiario": "PB",
                            "nome_beneficiario": "PREFEITURA DE SOUSA",
                            "cnpj_beneficiario": "08847784000144",
                            "id_ente": 1
                        }
                    ],
                    "total_pages": 2,
                    "total_items": 2,
                    "page_number": 1,
                    "page_size": 1
                }
                """;

        String page2Json = """
                {
                    "data": [
                        {
                            "id_beneficiario": 102,
                            "uf_beneficiario": "PB",
                            "nome_beneficiario": "PREFEITURA DE PATOS",
                            "cnpj_beneficiario": "09123456000188",
                            "id_ente": 2
                        }
                    ],
                    "total_pages": 2,
                    "total_items": 2,
                    "page_number": 2,
                    "page_size": 1
                }
                """;

        mockServer.expect(requestTo("http://localhost:8089/especiais/beneficiarios-especiais?uf_beneficiario=PB&pagina=1&tamanho_da_pagina=2"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(page1Json, MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo("http://localhost:8089/especiais/beneficiarios-especiais?uf_beneficiario=PB&pagina=2&tamanho_da_pagina=2"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(page2Json, MediaType.APPLICATION_JSON));

        List<BeneficiarioEspecialDTO> beneficiarios = client.consultarBeneficiarios("PB", null);

        assertThat(beneficiarios).hasSize(2);
        assertThat(beneficiarios.get(0).nomeBeneficiario()).isEqualTo("PREFEITURA DE SOUSA");
        assertThat(beneficiarios.get(1).nomeBeneficiario()).isEqualTo("PREFEITURA DE PATOS");
        mockServer.verify();
    }

    @Test
    @DisplayName("Deve recuperar com retry após falha transitória 503 e obter planos de ação")
    void deveRecuperarComRetryAposErro503() {
        String planosJson = """
                {
                    "data": [
                        {
                            "id_plano_acao": 5001,
                            "codigo_plano_acao": "2024.001",
                            "ano_plano_acao": 2024,
                            "situacao_plano_acao": "CIENTE",
                            "nome_parlamentar_emenda_plano_acao": "DEPUTADO JOAO",
                            "valor_custeio_plano_acao": 150000.00,
                            "valor_investimento_plano_acao": 350000.00,
                            "id_beneficiario": 101,
                            "nome_objeto": "Pavimentação Asfáltica Urbana"
                        }
                    ],
                    "total_pages": 1,
                    "total_items": 1,
                    "page_number": 1,
                    "page_size": 2
                }
                """;

        // 1ª tentativa: 503 Service Unavailable
        mockServer.expect(requestTo("http://localhost:8089/especiais/planos-acao-especiais?id_beneficiario=101&pagina=1&tamanho_da_pagina=2"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        // 2ª tentativa: Sucesso 200
        mockServer.expect(requestTo("http://localhost:8089/especiais/planos-acao-especiais?id_beneficiario=101&pagina=1&tamanho_da_pagina=2"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(planosJson, MediaType.APPLICATION_JSON));

        List<PlanoAcaoEspecialDTO> planos = client.consultarPlanosAcao(101L, null);

        assertThat(planos).hasSize(1);
        PlanoAcaoEspecialDTO plano = planos.get(0);
        assertThat(plano.idPlanoAcao()).isEqualTo(5001L);
        assertThat(plano.nomeParlamentarEmendaPlanoAcao()).isEqualTo("DEPUTADO JOAO");
        assertThat(plano.valorTotal()).isEqualByComparingTo(new BigDecimal("500000.00"));
        mockServer.verify();
    }

    @Test
    @DisplayName("Deve consultar planos de trabalho e relatórios de gestão vinculados")
    void deveConsultarPlanosTrabalhoERelatorios() {
        String ptJson = """
                {
                    "data": [
                        {
                            "id_plano_trabalho": 9001,
                            "id_plano_acao": 5001,
                            "situacao_plano_trabalho": "APROVADO",
                            "data_inicio_execucao_plano_trabalho": "2024-03-01",
                            "data_fim_execucao_plano_trabalho": "2024-12-31",
                            "prazo_execucao_meses_plano_trabalho": 10
                        }
                    ],
                    "total_pages": 1,
                    "total_items": 1,
                    "page_number": 1,
                    "page_size": 2
                }
                """;

        String rgJson = """
                {
                    "data": [
                        {
                            "id_relatorio_gestao_novo": 8001,
                            "id_plano_acao": 5001,
                            "tipo_relatorio_gestao_novo": "Final",
                            "situacao_relatorio_gestao_novo": "DISPONIBILIZADO",
                            "valor_executado_relatorio_gestao_novo": 500000.00,
                            "valor_pendente_relatorio_gestao_novo": 0.00
                        }
                    ],
                    "total_pages": 1,
                    "total_items": 1,
                    "page_number": 1,
                    "page_size": 2
                }
                """;

        mockServer.expect(requestTo("http://localhost:8089/especiais/planos-trabalho-especiais?id_plano_acao=5001&pagina=1&tamanho_da_pagina=2"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(ptJson, MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo("http://localhost:8089/especiais/relatorios-gestao-novos-especiais?id_plano_acao=5001&pagina=1&tamanho_da_pagina=2"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(rgJson, MediaType.APPLICATION_JSON));

        List<PlanoTrabalhoEspecialDTO> pts = client.consultarPlanosTrabalho(5001L);
        List<RelatorioGestaoEspecialDTO> rgs = client.consultarRelatoriosGestao(5001L);

        assertThat(pts).hasSize(1);
        assertThat(pts.get(0).isAprovado()).isTrue();

        assertThat(rgs).hasSize(1);
        assertThat(rgs.get(0).isDisponibilizado()).isTrue();
        assertThat(rgs.get(0).isFinal()).isTrue();

        mockServer.verify();
    }
}
