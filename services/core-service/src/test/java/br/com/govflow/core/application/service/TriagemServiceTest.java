package br.com.govflow.core.application.service;

import br.com.govflow.core.application.port.out.DocumentoRepositoryPort;
import br.com.govflow.core.domain.exception.AcessoNegadoException;
import br.com.govflow.core.domain.exception.TriagemItemNaoEncontradoException;
import br.com.govflow.core.domain.model.ArmazenamentoArquivo;
import br.com.govflow.core.domain.model.Documento;
import br.com.govflow.core.domain.model.documento.CategoriaDocumento;
import br.com.govflow.core.domain.model.documento.FaseCicloVida;
import br.com.govflow.core.domain.model.documento.OrigemCanal;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.request.CadastrarContatoETriarRequest;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.request.CadastrarContatoRequest;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.response.ContatoResponse;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.response.TriagemItemResponse;
import br.com.govflow.core.infrastructure.adapter.out.persistence.entity.ContatoConvenioCoreJpaEntity;
import br.com.govflow.core.infrastructure.adapter.out.persistence.entity.ContatoCoreJpaEntity;
import br.com.govflow.core.infrastructure.adapter.out.persistence.entity.ConvenioJpaEntity;
import br.com.govflow.core.infrastructure.adapter.out.persistence.entity.TriagemInboxJpaEntity;
import br.com.govflow.core.infrastructure.adapter.out.persistence.repository.SpringDataContatoConvenioCoreRepository;
import br.com.govflow.core.infrastructure.adapter.out.persistence.repository.SpringDataContatoCoreRepository;
import br.com.govflow.core.infrastructure.adapter.out.persistence.repository.SpringDataConvenioRepository;
import br.com.govflow.core.infrastructure.adapter.out.persistence.repository.SpringDataTriagemInboxRepository;
import br.com.govflow.core.infrastructure.interceptor.UserContext;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TriagemServiceTest {

    @Mock
    private SpringDataTriagemInboxRepository triagemInboxRepository;

    @Mock
    private SpringDataContatoCoreRepository contatoRepository;

    @Mock
    private SpringDataContatoConvenioCoreRepository contatoConvenioRepository;

    @Mock
    private SpringDataConvenioRepository convenioRepository;

    @Mock
    private DocumentoRepositoryPort documentoRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private TriagemService triagemService;

    private UUID tenantId;
    private UUID userId;
    private UUID prefeituraAutorizadaId;
    private UUID prefeituraNaoAutorizadaId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        prefeituraAutorizadaId = UUID.randomUUID();
        prefeituraNaoAutorizadaId = UUID.randomUUID();

        UserContext.setCurrentUser(userId, tenantId, Set.of("AGENTE"), Set.of(prefeituraAutorizadaId));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    @DisplayName("Deve listar pendentes respeitando restrição de prefeituras atribuídas ao agente")
    void deveListarPendentesRespeitandoEscopoDoAgente() {
        UUID convAutorizadoId = UUID.randomUUID();
        UUID convNaoAutorizadoId = UUID.randomUUID();

        TriagemInboxJpaEntity item1 = new TriagemInboxJpaEntity();
        item1.setId(UUID.randomUUID());
        item1.setTenantId(tenantId);
        item1.setConvenioSugeridoId(convAutorizadoId);
        item1.setStatus("PENDENTE");

        TriagemInboxJpaEntity item2 = new TriagemInboxJpaEntity();
        item2.setId(UUID.randomUUID());
        item2.setTenantId(tenantId);
        item2.setConvenioSugeridoId(convNaoAutorizadoId);
        item2.setStatus("PENDENTE");

        TriagemInboxJpaEntity itemNovo = new TriagemInboxJpaEntity();
        itemNovo.setId(UUID.randomUUID());
        itemNovo.setTenantId(tenantId);
        itemNovo.setConvenioSugeridoId(null);
        itemNovo.setRemetenteNovo(true);
        itemNovo.setStatus("PENDENTE");

        when(triagemInboxRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, "PENDENTE"))
                .thenReturn(List.of(item1, item2, itemNovo));

        ConvenioJpaEntity c1 = new ConvenioJpaEntity();
        c1.setId(convAutorizadoId);
        c1.setPrefeituraId(prefeituraAutorizadaId);
        c1.setTenantId(tenantId);
        c1.setNumeroSiconv("900001/2024");

        ConvenioJpaEntity c2 = new ConvenioJpaEntity();
        c2.setId(convNaoAutorizadoId);
        c2.setPrefeituraId(prefeituraNaoAutorizadaId);
        c2.setTenantId(tenantId);
        c2.setNumeroSiconv("900002/2024");

        when(convenioRepository.findById(convAutorizadoId)).thenReturn(Optional.of(c1));
        when(convenioRepository.findById(convNaoAutorizadoId)).thenReturn(Optional.of(c2));

        List<TriagemItemResponse> pendentes = triagemService.listarPendentes();

        assertThat(pendentes).hasSize(2);
        assertThat(pendentes).extracting(TriagemItemResponse::id)
                .containsExactlyInAnyOrder(item1.getId(), itemNovo.getId());
    }

    @Test
    @DisplayName("Deve cadastrar contato, vincular múltiplos convênios N:N e arquivar documento no GED")
    void deveCadastrarContatoETriarComSucesso() {
        UUID inboxId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        UUID convId = UUID.randomUUID();

        TriagemInboxJpaEntity inbox = new TriagemInboxJpaEntity();
        inbox.setId(inboxId);
        inbox.setTenantId(tenantId);
        inbox.setDocumentoId(docId);
        inbox.setStatus("PENDENTE");

        when(triagemInboxRepository.findByIdAndTenantId(inboxId, tenantId)).thenReturn(Optional.of(inbox));

        ConvenioJpaEntity convenio = new ConvenioJpaEntity();
        convenio.setId(convId);
        convenio.setTenantId(tenantId);
        convenio.setPrefeituraId(prefeituraAutorizadaId);
        convenio.setNumeroSiconv("888111/2024");
        when(convenioRepository.findById(convId)).thenReturn(Optional.of(convenio));

        when(contatoRepository.findByTenantIdAndPhoneNumber(tenantId, "5583999991111")).thenReturn(Optional.empty());
        when(contatoRepository.save(any(ContatoCoreJpaEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(triagemInboxRepository.save(any(TriagemInboxJpaEntity.class))).thenAnswer(i -> i.getArgument(0));

        Documento doc = Documento.criarNovo(
                docId,
                tenantId,
                prefeituraAutorizadaId,
                null,
                FaseCicloVida.FASE_04_EXECUCAO_FISICA,
                CategoriaDocumento.BOLETIM_MEDICAO,
                "/triagem",
                new ArmazenamentoArquivo("b", "k", "bm01.pdf", "application/pdf", 100L),
                "hash",
                OrigemCanal.WHATSAPP,
                null,
                null,
                null
        );
        when(documentoRepository.buscarPorId(docId)).thenReturn(Optional.of(doc));

        CadastrarContatoETriarRequest request = new CadastrarContatoETriarRequest(
                "Eng. Carlos Silva",
                "5583999991111",
                "FISCAL_ENGENHEIRO",
                "Construtora Alpha",
                List.of(convId),
                convId,
                "04_EXECUCAO_FISICA_E_MEDICOES",
                true
        );

        TriagemItemResponse response = triagemService.cadastrarContatoETriar(inboxId, request);

        assertThat(response.status()).isEqualTo("RESOLVIDO");
        assertThat(response.convenioSugeridoId()).isEqualTo(convId);

        // Verifica criação do contato
        ArgumentCaptor<ContatoCoreJpaEntity> contatoCaptor = ArgumentCaptor.forClass(ContatoCoreJpaEntity.class);
        verify(contatoRepository).save(contatoCaptor.capture());
        assertThat(contatoCaptor.getValue().getNome()).isEqualTo("Eng. Carlos Silva");

        // Verifica associação N:N
        verify(contatoConvenioRepository).save(any(ContatoConvenioCoreJpaEntity.class));

        // Verifica arquivamento do documento na pasta correspondente
        ArgumentCaptor<Documento> docCaptor = ArgumentCaptor.forClass(Documento.class);
        verify(documentoRepository).salvar(docCaptor.capture());
        Documento docArquivado = docCaptor.getValue();
        assertThat(docArquivado.getConvenioId()).isEqualTo(convId);
        assertThat(docArquivado.getPastaVirtual()).isEqualTo("/04_Execucao_Fisica_e_Medicoes");
    }

    @Test
    @DisplayName("Deve impedir agente de vincular convênio de prefeitura que ele não possui acesso")
    void deveImpedirAgenteDeVincularConvenioNaoAutorizado() {
        UUID inboxId = UUID.randomUUID();
        UUID convNaoAutorizadoId = UUID.randomUUID();

        TriagemInboxJpaEntity inbox = new TriagemInboxJpaEntity();
        inbox.setId(inboxId);
        inbox.setTenantId(tenantId);
        inbox.setStatus("PENDENTE");

        when(triagemInboxRepository.findByIdAndTenantId(inboxId, tenantId)).thenReturn(Optional.of(inbox));

        ConvenioJpaEntity convInvalido = new ConvenioJpaEntity();
        convInvalido.setId(convNaoAutorizadoId);
        convInvalido.setTenantId(tenantId);
        convInvalido.setPrefeituraId(prefeituraNaoAutorizadaId);

        when(convenioRepository.findById(convNaoAutorizadoId)).thenReturn(Optional.of(convInvalido));
        when(contatoRepository.findByTenantIdAndPhoneNumber(any(), any())).thenReturn(Optional.empty());
        when(contatoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        CadastrarContatoETriarRequest request = new CadastrarContatoETriarRequest(
                "Fiscal Estranho",
                "5583999992222",
                "FISCAL",
                "Empresa",
                List.of(convNaoAutorizadoId),
                convNaoAutorizadoId,
                "04_EXECUCAO_FISICA_E_MEDICOES",
                false
        );

        assertThatThrownBy(() -> triagemService.cadastrarContatoETriar(inboxId, request))
                .isInstanceOf(AcessoNegadoException.class)
                .hasMessageContaining("Agente não possui acesso");
    }

    @Test
    @DisplayName("Deve confirmar arquivamento em 1 clique movendo documento e marcando RESOLVIDO")
    void deveConfirmarArquivamentoEm1Clique() {
        UUID inboxId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        UUID convId = UUID.randomUUID();

        TriagemInboxJpaEntity inbox = new TriagemInboxJpaEntity();
        inbox.setId(inboxId);
        inbox.setTenantId(tenantId);
        inbox.setDocumentoId(docId);
        inbox.setConvenioSugeridoId(convId);
        inbox.setFaseSugerida("04_EXECUCAO_FISICA_E_MEDICOES");
        inbox.setStatus("PENDENTE");

        when(triagemInboxRepository.findByIdAndTenantId(inboxId, tenantId)).thenReturn(Optional.of(inbox));

        ConvenioJpaEntity convenio = new ConvenioJpaEntity();
        convenio.setId(convId);
        convenio.setTenantId(tenantId);
        convenio.setPrefeituraId(prefeituraAutorizadaId);
        convenio.setNumeroSiconv("888222/2024");
        when(convenioRepository.findById(convId)).thenReturn(Optional.of(convenio));
        when(triagemInboxRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Documento doc = Documento.criarNovo(
                docId,
                tenantId,
                prefeituraAutorizadaId,
                null,
                FaseCicloVida.FASE_04_EXECUCAO_FISICA,
                CategoriaDocumento.BOLETIM_MEDICAO,
                "/triagem",
                new ArmazenamentoArquivo("b", "k", "bm.pdf", "application/pdf", 100L),
                "hash",
                OrigemCanal.WHATSAPP,
                null,
                null,
                null
        );
        when(documentoRepository.buscarPorId(docId)).thenReturn(Optional.of(doc));

        TriagemItemResponse response = triagemService.confirmarArquivamento(inboxId, convId, "04_EXECUCAO_FISICA_E_MEDICOES");

        assertThat(response.status()).isEqualTo("RESOLVIDO");
        verify(documentoRepository).salvar(doc);
        assertThat(doc.getPastaVirtual()).isEqualTo("/04_Execucao_Fisica_e_Medicoes");
    }

    @Test
    @DisplayName("Deve marcar item como IGNORADO")
    void deveIgnorarItem() {
        UUID inboxId = UUID.randomUUID();
        TriagemInboxJpaEntity inbox = new TriagemInboxJpaEntity();
        inbox.setId(inboxId);
        inbox.setTenantId(tenantId);
        inbox.setStatus("PENDENTE");

        when(triagemInboxRepository.findByIdAndTenantId(inboxId, tenantId)).thenReturn(Optional.of(inbox));
        when(triagemInboxRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        TriagemItemResponse response = triagemService.ignorarItem(inboxId);

        assertThat(response.status()).isEqualTo("IGNORADO");
        assertThat(inbox.getStatus()).isEqualTo("IGNORADO");
        assertThat(inbox.getResolvidoEm()).isNotNull();
    }
}
