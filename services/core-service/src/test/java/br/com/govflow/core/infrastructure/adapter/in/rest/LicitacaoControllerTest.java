package br.com.govflow.core.infrastructure.adapter.in.rest;

import br.com.govflow.core.application.port.in.CadastrarLicitacaoUseCase;
import br.com.govflow.core.application.port.in.ConsultarLicitacoesConvenioUseCase;
import br.com.govflow.core.application.port.in.EmitirAioUseCase;
import br.com.govflow.core.application.port.in.HomologarLicitacaoUseCase;
import br.com.govflow.core.application.port.in.RegistrarResultadoVrplUseCase;
import br.com.govflow.core.application.port.in.SubmeterVrplUseCase;
import br.com.govflow.core.application.port.in.ValidarAioParaMedicaoUseCase;
import br.com.govflow.core.domain.exception.AioNaoEmitidaException;
import br.com.govflow.core.domain.model.licitacao.CriterioJulgamento;
import br.com.govflow.core.domain.model.licitacao.Licitacao;
import br.com.govflow.core.domain.model.licitacao.ModalidadeLicitacao;
import br.com.govflow.core.domain.model.licitacao.RegimeExecucao;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.request.CadastrarLicitacaoRequest;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.request.EmitirAioRequest;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.request.HomologarLicitacaoRequest;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.request.RegistrarResultadoVrplRequest;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.request.SubmeterVrplRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LicitacaoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CadastrarLicitacaoUseCase cadastrarUseCase;

    @MockBean
    private ConsultarLicitacoesConvenioUseCase consultarUseCase;

    @MockBean
    private HomologarLicitacaoUseCase homologarUseCase;

    @MockBean
    private SubmeterVrplUseCase submeterVrplUseCase;

    @MockBean
    private RegistrarResultadoVrplUseCase registrarResultadoVrplUseCase;

    @MockBean
    private EmitirAioUseCase emitirAioUseCase;

    @MockBean
    private ValidarAioParaMedicaoUseCase validarAioParaMedicaoUseCase;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID convenioId = UUID.randomUUID();
    private final UUID prefeituraId = UUID.randomUUID();

    private Licitacao licitacaoDeObras() {
        return Licitacao.cadastrar(
                tenantId, convenioId, prefeituraId, "001/2026", "Concorrência 002/2026",
                ModalidadeLicitacao.CONCORRENCIA_ELETRONICA, CriterioJulgamento.MENOR_PRECO,
                RegimeExecucao.EMPREITADA_PRECO_UNITARIO, "Obras civis",
                new BigDecimal("2000000.00"), LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 20),
                null, null, null
        );
    }

    @Test
    @DisplayName("Deve cadastrar licitação retornando 201")
    void deveCadastrarLicitacao() throws Exception {
        Licitacao salva = licitacaoDeObras();
        when(cadastrarUseCase.cadastrar(any())).thenReturn(salva);

        CadastrarLicitacaoRequest request = new CadastrarLicitacaoRequest(
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

        mockMvc.perform(post("/api/v1/convenios/" + convenioId + "/licitacoes")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroLicitacao").value("Concorrência 002/2026"))
                .andExpect(jsonPath("$.statusVrpl").value("NAO_ENVIADO"))
                .andExpect(jsonPath("$.statusAio").value("NAO_EMITIDO"))
                .andExpect(jsonPath("$.podeCriarMedicao").value(false));
    }

    @Test
    @DisplayName("Deve listar licitações 1:N do convênio")
    void deveListarLicitacoes() throws Exception {
        when(consultarUseCase.listarPorConvenio(convenioId)).thenReturn(List.of(licitacaoDeObras()));

        mockMvc.perform(get("/api/v1/convenios/" + convenioId + "/licitacoes")
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numeroLicitacao").value("Concorrência 002/2026"));
    }

    @Test
    @DisplayName("Deve homologar, submeter VRPL, registrar aceite e emitir AIO")
    void devePercorrerEndpointsDeVrplEAio() throws Exception {
        Licitacao licitacao = licitacaoDeObras();
        licitacao.homologar(new BigDecimal("1800000.00"), LocalDate.of(2026, 4, 10),
                "12.345.678/0001-90", "Construtora Alvorada Ltda");
        when(homologarUseCase.homologar(any())).thenReturn(licitacao);

        mockMvc.perform(post("/api/v1/convenios/" + convenioId + "/licitacoes/" + licitacao.getId() + "/homologar")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new HomologarLicitacaoRequest(
                                new BigDecimal("1800000.00"), LocalDate.of(2026, 4, 10),
                                "12.345.678/0001-90", "Construtora Alvorada Ltda"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.percentualDesconto").value(10.00));

        licitacao.submeterVrpl("VRPL-01", LocalDate.of(2026, 4, 15));
        when(submeterVrplUseCase.submeter(any())).thenReturn(licitacao);
        mockMvc.perform(post("/api/v1/convenios/" + convenioId + "/licitacoes/" + licitacao.getId() + "/vrpl/submeter")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubmeterVrplRequest("VRPL-01", LocalDate.of(2026, 4, 15)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusVrpl").value("EM_ANALISE_CAIXA"));

        licitacao.aceitarVrpl("Parecer favorável.", LocalDate.of(2026, 5, 2));
        when(registrarResultadoVrplUseCase.registrar(any())).thenReturn(licitacao);
        mockMvc.perform(post("/api/v1/convenios/" + convenioId + "/licitacoes/" + licitacao.getId() + "/vrpl/resultado")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegistrarResultadoVrplRequest(
                                br.com.govflow.core.domain.model.licitacao.StatusVrpl.ACEITO_HOMOLOGADO,
                                "Parecer favorável.", null, LocalDate.of(2026, 5, 2)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusVrpl").value("ACEITO_HOMOLOGADO"));

        licitacao.emitirAio("AIO-003/2026", LocalDate.of(2026, 5, 20));
        when(emitirAioUseCase.emitir(any())).thenReturn(licitacao);
        mockMvc.perform(post("/api/v1/convenios/" + convenioId + "/licitacoes/" + licitacao.getId() + "/aio")
                        .header("X-Tenant-Id", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EmitirAioRequest("AIO-003/2026", LocalDate.of(2026, 5, 20)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusAio").value("EMITIDO"))
                .andExpect(jsonPath("$.podeCriarMedicao").value(true));
    }

    @Test
    @DisplayName("Validar AIO para medição retorna 204 quando emitida e 422 quando bloqueada")
    void deveValidarTravaAntigrosaViaEndpoint() throws Exception {
        Licitacao licitacao = licitacaoDeObras();
        when(consultarUseCase.buscarPorId(convenioId, licitacao.getId())).thenReturn(licitacao);
        doThrow(new AioNaoEmitidaException(licitacao.getId()))
                .when(validarAioParaMedicaoUseCase).garantirAioEmitida(licitacao.getId());

        mockMvc.perform(post("/api/v1/convenios/" + convenioId + "/licitacoes/" + licitacao.getId() + "/validar-aio-medicao")
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("AIO_NAO_EMITIDA"));

        doNothing().when(validarAioParaMedicaoUseCase).garantirAioEmitida(licitacao.getId());
        mockMvc.perform(post("/api/v1/convenios/" + convenioId + "/licitacoes/" + licitacao.getId() + "/validar-aio-medicao")
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isNoContent());
    }
}
