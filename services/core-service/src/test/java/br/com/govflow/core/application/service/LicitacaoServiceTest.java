package br.com.govflow.core.application.service;

import br.com.govflow.core.application.port.in.CadastrarLicitacaoUseCase;
import br.com.govflow.core.application.port.in.EmitirAioUseCase;
import br.com.govflow.core.application.port.in.HomologarLicitacaoUseCase;
import br.com.govflow.core.application.port.in.RegistrarResultadoVrplUseCase;
import br.com.govflow.core.application.port.in.SubmeterVrplUseCase;
import br.com.govflow.core.application.port.out.ConvenioRepositoryPort;
import br.com.govflow.core.application.port.out.LicitacaoRepositoryPort;
import br.com.govflow.core.application.port.out.UsuarioRepositoryPort;
import br.com.govflow.core.domain.exception.AcessoNegadoException;
import br.com.govflow.core.domain.exception.AioNaoEmitidaException;
import br.com.govflow.core.domain.model.convenio.Convenio;
import br.com.govflow.core.domain.model.licitacao.CriterioJulgamento;
import br.com.govflow.core.domain.model.licitacao.Licitacao;
import br.com.govflow.core.domain.model.licitacao.ModalidadeLicitacao;
import br.com.govflow.core.domain.model.licitacao.RegimeExecucao;
import br.com.govflow.core.domain.model.licitacao.StatusAio;
import br.com.govflow.core.domain.model.licitacao.StatusVrpl;
import br.com.govflow.core.infrastructure.interceptor.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LicitacaoServiceTest {

    @Mock
    private ConvenioRepositoryPort convenioRepository;

    @Mock
    private LicitacaoRepositoryPort licitacaoRepository;

    @Mock
    private UsuarioRepositoryPort usuarioRepository;

    private LicitacaoService service;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID prefeituraId = UUID.randomUUID();
    private final UUID convenioId = UUID.randomUUID();
    private Convenio convenio;

    @BeforeEach
    void setUp() {
        service = new LicitacaoService(convenioRepository, licitacaoRepository, usuarioRepository);
        convenio = new Convenio(
                convenioId,
                tenantId,
                prefeituraId,
                "914250/2023",
                "00124/2023",
                "FNDE / MEC",
                "Construção de Creche Proinfância Tipo 2",
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
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    private CadastrarLicitacaoUseCase.CadastrarLicitacaoCommand comandoCadastro(String numero) {
        return new CadastrarLicitacaoUseCase.CadastrarLicitacaoCommand(
                convenioId,
                "001/2026",
                numero,
                ModalidadeLicitacao.CONCORRENCIA_ELETRONICA,
                CriterioJulgamento.MENOR_PRECO,
                RegimeExecucao.EMPREITADA_PRECO_UNITARIO,
                "Obras civis",
                new BigDecimal("2000000.00"),
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 20),
                null,
                null,
                null
        );
    }

    @Test
    @DisplayName("Deve cadastrar múltiplas licitações 1:N no mesmo convênio")
    void deveCadastrarMultiplasLicitacoesNoMesmoConvenio() {
        when(convenioRepository.buscarPorId(convenioId)).thenReturn(Optional.of(convenio));
        when(licitacaoRepository.existePorConvenioIdENumeroLicitacao(any(), any())).thenReturn(false);
        when(licitacaoRepository.salvar(any(Licitacao.class))).thenAnswer(inv -> inv.getArgument(0));

        Licitacao obras = service.cadastrar(comandoCadastro("Concorrência 002/2026"));
        Licitacao supervisao = service.cadastrar(comandoCadastro("Pregão 015/2026"));

        assertThat(obras.getConvenioId()).isEqualTo(convenioId);
        assertThat(supervisao.getConvenioId()).isEqualTo(convenioId);
        assertThat(obras.getNumeroLicitacao()).isNotEqualTo(supervisao.getNumeroLicitacao());
        assertThat(obras.getStatusVrpl()).isEqualTo(StatusVrpl.NAO_ENVIADO);
        assertThat(obras.getStatusAio()).isEqualTo(StatusAio.NAO_EMITIDO);
    }

    @Test
    @DisplayName("Deve homologar, submeter VRPL, aceitar parecer e emitir AIO")
    void devePercorrerCicloVrplEAio() {
        when(convenioRepository.buscarPorId(convenioId)).thenReturn(Optional.of(convenio));
        when(licitacaoRepository.salvar(any(Licitacao.class))).thenAnswer(inv -> inv.getArgument(0));

        Licitacao licitacao = Licitacao.cadastrar(
                tenantId, convenioId, prefeituraId, "001/2026", "Concorrência 002/2026",
                ModalidadeLicitacao.CONCORRENCIA_ELETRONICA, CriterioJulgamento.MENOR_PRECO,
                RegimeExecucao.EMPREITADA_PRECO_UNITARIO, "Obras civis",
                new BigDecimal("2000000.00"), LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 20),
                null, null, null
        );
        when(licitacaoRepository.buscarPorId(licitacao.getId())).thenReturn(Optional.of(licitacao));

        service.homologar(new HomologarLicitacaoUseCase.HomologarLicitacaoCommand(
                convenioId, licitacao.getId(), new BigDecimal("1800000.00"),
                LocalDate.of(2026, 4, 10), "12.345.678/0001-90", "Construtora Alvorada Ltda"
        ));
        service.submeter(new SubmeterVrplUseCase.SubmeterVrplCommand(
                convenioId, licitacao.getId(), "VRPL-914250-01", LocalDate.of(2026, 4, 15)
        ));
        service.registrar(new RegistrarResultadoVrplUseCase.RegistrarResultadoVrplCommand(
                convenioId, licitacao.getId(), StatusVrpl.ACEITO_HOMOLOGADO,
                "Planilha compatível com a SPA.", null, LocalDate.of(2026, 5, 2)
        ));
        Licitacao comAio = service.emitir(new EmitirAioUseCase.EmitirAioCommand(
                convenioId, licitacao.getId(), "AIO-003/2026", LocalDate.of(2026, 5, 20)
        ));

        assertThat(comAio.getPercentualDesconto()).isEqualByComparingTo("10.00");
        assertThat(comAio.getStatusVrpl()).isEqualTo(StatusVrpl.ACEITO_HOMOLOGADO);
        assertThat(comAio.getParecerTecnicoVrpl()).contains("SPA");
        assertThat(comAio.getStatusAio()).isEqualTo(StatusAio.EMITIDO);
        service.garantirAioEmitida(licitacao.getId());
    }

    @Test
    @DisplayName("TravaAntigrosa: garantirAioEmitida falha sem AIO emitida")
    void deveBloquearMedicaoSemAio() {
        Licitacao licitacao = Licitacao.cadastrar(
                tenantId, convenioId, prefeituraId, "001/2026", "Concorrência 002/2026",
                ModalidadeLicitacao.CONCORRENCIA_ELETRONICA, CriterioJulgamento.MENOR_PRECO,
                RegimeExecucao.EMPREITADA_PRECO_UNITARIO, "Obras civis",
                new BigDecimal("2000000.00"), null, null, null, null, null
        );
        when(licitacaoRepository.buscarPorId(licitacao.getId())).thenReturn(Optional.of(licitacao));

        assertThatThrownBy(() -> service.garantirAioEmitida(licitacao.getId()))
                .isInstanceOf(AioNaoEmitidaException.class);
    }

    @Test
    @DisplayName("Agente sem prefeitura vinculada recebe 403")
    void deveNegarAcessoDeAgenteForaDoEscopo() {
        UUID userId = UUID.randomUUID();
        UserContext.setCurrentUser(userId, tenantId, Set.of("AGENTE"), Set.of());
        when(convenioRepository.buscarPorId(convenioId)).thenReturn(Optional.of(convenio));
        when(usuarioRepository.buscarPrefeiturasAtribuidas(userId)).thenReturn(Set.of());

        assertThatThrownBy(() -> service.listarPorConvenio(convenioId))
                .isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    @DisplayName("Deve listar as licitações do convênio após cadastro")
    void deveListarPorConvenio() {
        when(convenioRepository.buscarPorId(convenioId)).thenReturn(Optional.of(convenio));
        Licitacao obras = Licitacao.cadastrar(
                tenantId, convenioId, prefeituraId, "001/2026", "Concorrência 002/2026",
                ModalidadeLicitacao.CONCORRENCIA_ELETRONICA, CriterioJulgamento.MENOR_PRECO,
                RegimeExecucao.EMPREITADA_PRECO_UNITARIO, "Obras civis",
                new BigDecimal("2000000.00"), null, null, null, null, null
        );
        when(licitacaoRepository.listarPorConvenioId(convenioId)).thenReturn(List.of(obras));

        List<Licitacao> lista = service.listarPorConvenio(convenioId);
        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).getNumeroLicitacao()).isEqualTo("Concorrência 002/2026");
    }
}
