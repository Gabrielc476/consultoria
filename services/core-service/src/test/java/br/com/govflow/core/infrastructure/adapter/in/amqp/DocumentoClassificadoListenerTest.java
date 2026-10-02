package br.com.govflow.core.infrastructure.adapter.in.amqp;

import br.com.govflow.core.application.port.out.DocumentoRepositoryPort;
import br.com.govflow.core.domain.model.Documento;
import br.com.govflow.core.domain.model.documento.CategoriaDocumento;
import br.com.govflow.core.domain.model.documento.DocumentoAuditoria;
import br.com.govflow.core.domain.model.documento.FaseCicloVida;
import br.com.govflow.core.domain.model.documento.OrigemCanal;
import br.com.govflow.core.infrastructure.adapter.in.amqp.dto.DocumentoClassificadoEventDto;
import br.com.govflow.core.infrastructure.adapter.out.persistence.entity.TriagemInboxJpaEntity;
import br.com.govflow.core.infrastructure.adapter.out.persistence.repository.SpringDataConsultoriaRepository;
import br.com.govflow.core.infrastructure.adapter.out.persistence.repository.SpringDataTriagemInboxRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentoClassificadoListenerTest {

    @Mock
    private DocumentoRepositoryPort documentoRepository;

    @Mock
    private SpringDataTriagemInboxRepository triagemInboxRepository;

    @Mock
    private SpringDataConsultoriaRepository consultoriaRepository;

    @InjectMocks
    private DocumentoClassificadoListener listener;

    @Test
    @DisplayName("Deve arquivar documento na pasta da fase e registrar auditoria quando confiança > 0.90")
    void deveArquivarDocumentoNaPastaDiretaComAltaConfianca() {
        UUID eventId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID prefeituraId = UUID.randomUUID();
        UUID convenioId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();

        DocumentoClassificadoEventDto event = new DocumentoClassificadoEventDto(
                eventId,
                correlationId,
                tenantId,
                prefeituraId,
                convenioId,
                null,
                "govflow-docs",
                "tenants/1/fase5/nf123.pdf",
                "nf123.pdf",
                "application/pdf",
                1024L,
                "sha256abc123",
                "05_Execucao_Financeira_e_Pagamentos",
                "DOCUMENTO_HABIL",
                null,
                new BigDecimal("0.96"),
                "+5511999998888",
                List.of("nf", "obra"),
                Map.of("fornecedor", "Construtora Alfa"),
                Instant.now()
        );

        when(documentoRepository.salvar(any(Documento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        listener.onDocumentoClassificado(event);

        ArgumentCaptor<Documento> docCaptor = ArgumentCaptor.forClass(Documento.class);
        verify(documentoRepository).salvar(docCaptor.capture());
        Documento docSalvo = docCaptor.getValue();

        assertThat(docSalvo.getFaseCicloVida()).isEqualTo(FaseCicloVida.FASE_05_EXECUCAO_FINANCEIRA);
        assertThat(docSalvo.getCategoriaDocumento()).isEqualTo(CategoriaDocumento.DOCUMENTO_HABIL);
        assertThat(docSalvo.getPastaVirtual()).isEqualTo("/05_Execucao_Financeira_e_Pagamentos");
        assertThat(docSalvo.getOrigemCanal()).isEqualTo(OrigemCanal.WHATSAPP);
        assertThat(docSalvo.getHashSha256()).isEqualTo("sha256abc123");

        ArgumentCaptor<DocumentoAuditoria> auditCaptor = ArgumentCaptor.forClass(DocumentoAuditoria.class);
        verify(documentoRepository).salvarAuditoria(auditCaptor.capture());
        DocumentoAuditoria audit = auditCaptor.getValue();

        assertThat(audit.getAcao()).isEqualTo("CLASSIFICACAO_IA");
        assertThat(audit.getJustificativa()).contains("0.96");
        verify(triagemInboxRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve rotear documento para /triagem quando confiança for menor ou igual a 0.90")
    void deveRotearParaTriagemComBaixaConfianca() {
        UUID eventId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID prefeituraId = UUID.randomUUID();
        UUID convenioId = UUID.randomUUID();

        DocumentoClassificadoEventDto event = new DocumentoClassificadoEventDto(
                eventId,
                UUID.randomUUID(),
                tenantId,
                prefeituraId,
                convenioId,
                null,
                "govflow-docs",
                "tenants/1/fase1/plano.pdf",
                "plano.pdf",
                "application/pdf",
                2048L,
                "hash75",
                "01_Celebracao_e_Formalizacao",
                "PROPOSTA_PLANO_TRABALHO",
                null,
                new BigDecimal("0.75"),
                "+5511999997777",
                null,
                null,
                Instant.now()
        );

        when(documentoRepository.salvar(any(Documento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        listener.onDocumentoClassificado(event);

        ArgumentCaptor<Documento> docCaptor = ArgumentCaptor.forClass(Documento.class);
        verify(documentoRepository).salvar(docCaptor.capture());
        Documento docSalvo = docCaptor.getValue();

        assertThat(docSalvo.getPastaVirtual()).isEqualTo("/triagem");

        ArgumentCaptor<DocumentoAuditoria> auditCaptor = ArgumentCaptor.forClass(DocumentoAuditoria.class);
        verify(documentoRepository).salvarAuditoria(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getAcao()).isEqualTo("ENCAMINHADO_TRIAGEM");
        verify(triagemInboxRepository).save(any(TriagemInboxJpaEntity.class));
    }

    @Test
    @DisplayName("Deve lançar AmqpRejectAndDontRequeueException para mensagem sem tenantId")
    void deveRejeitarMensagemSemTenant() {
        DocumentoClassificadoEventDto event = new DocumentoClassificadoEventDto(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                null,
                null,
                null,
                "b", "k", "f", "m", 1L, "h",
                "01_Celebracao",
                "OUTROS",
                null,
                BigDecimal.ONE,
                null, null, null, Instant.now()
        );

        assertThatThrownBy(() -> listener.onDocumentoClassificado(event))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);

        verify(documentoRepository, never()).salvar(any());
    }

    @Test
    @DisplayName("Regra Mandatória: Deve descartar documento e não criar na triagem quando remetente não for contato vinculado")
    void deveDescartarDocumentoQuandoRemetenteNaoVinculado() {
        UUID eventId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        // Evento com remetenteNovo = true
        DocumentoClassificadoEventDto event = new DocumentoClassificadoEventDto(
                eventId,
                UUID.randomUUID(),
                tenantId,
                null,
                null,
                UUID.randomUUID(),
                "bucket",
                "key",
                "arquivo.pdf",
                "application/pdf",
                1024L,
                "hash",
                "04_EXECUCAO_FISICA",
                "OUTROS",
                null,
                new BigDecimal("0.50"),
                "+5583999990000",
                null,
                null,
                Instant.now(),
                null,
                "Desconhecido",
                true, // remetenteNovo = true
                "Remetente não vinculado",
                "Resumo",
                true, // direcionarTriagem = true
                null
        );

        listener.onDocumentoClassificado(event);

        verify(documentoRepository, never()).salvar(any());
        verify(documentoRepository, never()).salvarAuditoria(any());
        verify(triagemInboxRepository, never()).save(any());
    }
}
