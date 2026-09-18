package br.com.govflow.core.application.service;

import br.com.govflow.core.application.port.in.AprovarDocumentoUseCase;
import br.com.govflow.core.application.port.in.RejeitarDocumentoUseCase;
import br.com.govflow.core.application.port.out.AuditoriaRevisaoRepositoryPort;
import br.com.govflow.core.application.port.out.DocumentoEventPublisherPort;
import br.com.govflow.core.application.port.out.DocumentoRepositoryPort;
import br.com.govflow.core.domain.event.DocumentoProntoParaTransferegovEvent;
import br.com.govflow.core.domain.event.DocumentoRejeitadoEvent;
import br.com.govflow.core.domain.exception.DocumentoNaoEncontradoException;
import br.com.govflow.core.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RevisaoDocumentoServiceTest {

    @Mock
    private DocumentoRepositoryPort documentoRepository;

    @Mock
    private AuditoriaRevisaoRepositoryPort auditoriaRepository;

    @Mock
    private DocumentoEventPublisherPort eventPublisher;

    @InjectMocks
    private RevisaoDocumentoService service;

    private UUID tenantId;
    private UUID analistaId;
    private UUID documentoId;
    private Documento documentoEmConferencia;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        analistaId = UUID.randomUUID();
        documentoId = UUID.randomUUID();

        documentoEmConferencia = Documento.criarRecebido(
                documentoId,
                tenantId,
                null,
                null,
                "bucket",
                "key",
                "doc.pdf",
                "application/pdf",
                1000L
        );
        documentoEmConferencia.registrarExtracaoIA(
                new ExtracaoSugerida(
                        TipoDocumentoHabil.NOTA_FISCAL_SERVICOS,
                        "123",
                        "1",
                        null,
                        LocalDate.of(2026, 5, 10),
                        "12345678000195",
                        "Razao XPTO",
                        "Serviços",
                        null,
                        new BigDecimal("2000.00"),
                        BigDecimal.ZERO,
                        new BigDecimal("2000.00"),
                        List.of(),
                        0.95,
                        true,
                        List.of()
                ),
                Map.of()
        );
    }

    @Test
    @DisplayName("Deve aprovar documento, salvar auditoria e publicar DocumentoProntoParaTransferegovEvent")
    void deveAprovarDocumentoComSucesso() {
        when(documentoRepository.buscarPorId(documentoId)).thenReturn(Optional.of(documentoEmConferencia));
        when(documentoRepository.salvar(any(Documento.class))).thenAnswer(i -> i.getArgument(0));
        when(auditoriaRepository.salvar(any(AuditoriaRevisao.class))).thenAnswer(i -> i.getArgument(0));

        DadosRevisaoAnalista revisao = new DadosRevisaoAnalista(
                TipoDocumentoHabil.NOTA_FISCAL_SERVICOS,
                "123",
                "1",
                null,
                LocalDate.of(2026, 5, 10),
                "12345678000195",
                "Razao XPTO",
                "Serviços",
                null,
                new BigDecimal("2000.00"),
                BigDecimal.ZERO,
                new BigDecimal("2000.00"),
                List.of(),
                "Aprovado sem alterações"
        );

        AprovarDocumentoUseCase.AprovarDocumentoCommand command =
                new AprovarDocumentoUseCase.AprovarDocumentoCommand(documentoId, analistaId, revisao, "Ok");

        AprovarDocumentoUseCase.ResultadoAprovacao resultado = service.aprovar(command);

        assertNotNull(resultado);
        assertEquals(StatusDocumento.PRONTO_PARA_TRANSFEREGOV, resultado.documento().getStatus());
        assertEquals(AcaoAuditoria.APROVACAO, resultado.auditoria().getAcao());

        verify(documentoRepository).salvar(documentoEmConferencia);
        verify(auditoriaRepository).salvar(any(AuditoriaRevisao.class));
        verify(eventPublisher).publicarDocumentoPronto(any(DocumentoProntoParaTransferegovEvent.class));
    }

    @Test
    @DisplayName("Deve rejeitar documento, salvar auditoria e publicar DocumentoRejeitadoEvent")
    void deveRejeitarDocumentoComSucesso() {
        when(documentoRepository.buscarPorId(documentoId)).thenReturn(Optional.of(documentoEmConferencia));
        when(documentoRepository.salvar(any(Documento.class))).thenAnswer(i -> i.getArgument(0));
        when(auditoriaRepository.salvar(any(AuditoriaRevisao.class))).thenAnswer(i -> i.getArgument(0));

        RejeitarDocumentoUseCase.RejeitarDocumentoCommand command =
                new RejeitarDocumentoUseCase.RejeitarDocumentoCommand(documentoId, analistaId, "Documento ilegível");

        RejeitarDocumentoUseCase.ResultadoRejeicao resultado = service.rejeitar(command);

        assertNotNull(resultado);
        assertEquals(StatusDocumento.REJEITADO, resultado.documento().getStatus());
        assertEquals("Documento ilegível", resultado.documento().getMotivoRejeicao());

        verify(documentoRepository).salvar(documentoEmConferencia);
        verify(auditoriaRepository).salvar(any(AuditoriaRevisao.class));
        verify(eventPublisher).publicarDocumentoRejeitado(any(DocumentoRejeitadoEvent.class));
    }

    @Test
    @DisplayName("Deve lançar DocumentoNaoEncontradoException quando documento não existir")
    void deveLancarExcecaoQuandoDocumentoNaoExistir() {
        when(documentoRepository.buscarPorId(documentoId)).thenReturn(Optional.empty());

        RejeitarDocumentoUseCase.RejeitarDocumentoCommand command =
                new RejeitarDocumentoUseCase.RejeitarDocumentoCommand(documentoId, analistaId, "Motivo");

        assertThrows(DocumentoNaoEncontradoException.class, () -> service.rejeitar(command));
        verifyNoInteractions(auditoriaRepository, eventPublisher);
    }
}
