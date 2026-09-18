package br.com.govflow.core.application.service;

import br.com.govflow.core.application.port.in.ProcessarDocumentoExtraidoUseCase;
import br.com.govflow.core.application.port.out.DocumentoRepositoryPort;
import br.com.govflow.core.domain.model.BoundingBox;
import br.com.govflow.core.domain.model.Documento;
import br.com.govflow.core.domain.model.ExtracaoSugerida;
import br.com.govflow.core.domain.model.StatusDocumento;
import br.com.govflow.core.domain.model.TipoDocumentoHabil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class ProcessarDocumentoExtraidoServiceTest {

    @Mock
    private DocumentoRepositoryPort documentoRepository;

    @InjectMocks
    private ProcessarDocumentoExtraidoService service;

    private UUID tenantId;
    private UUID documentoId;
    private ExtracaoSugerida extracao;
    private Map<String, BoundingBox> boxes;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        documentoId = UUID.randomUUID();
        extracao = new ExtracaoSugerida(
                TipoDocumentoHabil.NOTA_FISCAL_SERVICOS,
                "000123",
                "1",
                null,
                LocalDate.of(2026, 5, 10),
                "12345678000190",
                "Empreiteira XPTO",
                "Serviço de Obra",
                null,
                new BigDecimal("5000.00"),
                BigDecimal.ZERO,
                new BigDecimal("5000.00"),
                List.of(),
                0.98,
                true,
                List.of()
        );
        boxes = Map.of("numeroDocumento", new BoundingBox(0.1, 0.1, 0.2, 0.2));
    }

    @Test
    @DisplayName("Deve criar novo documento e transicionar para EM_CONFERENCIA quando não existir no banco")
    void deveCriarNovoDocumentoQuandoNaoExistir() {
        when(documentoRepository.buscarPorId(documentoId)).thenReturn(Optional.empty());
        when(documentoRepository.salvar(any(Documento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProcessarDocumentoExtraidoUseCase.ProcessarDocumentoExtraidoCommand command =
                new ProcessarDocumentoExtraidoUseCase.ProcessarDocumentoExtraidoCommand(
                        tenantId,
                        documentoId,
                        UUID.randomUUID(),
                        null,
                        "bucket-govflow",
                        "docs/doc.pdf",
                        "doc.pdf",
                        "application/pdf",
                        2048L,
                        extracao,
                        boxes
                );

        Documento resultado = service.processar(command);

        assertNotNull(resultado);
        assertEquals(documentoId, resultado.getId());
        assertEquals(StatusDocumento.EM_CONFERENCIA, resultado.getStatus());
        assertEquals("000123", resultado.getExtracaoSugerida().numeroDocumento());
        assertEquals(1, resultado.getBoundingBoxes().size());

        verify(documentoRepository).salvar(any(Documento.class));
    }

    @Test
    @DisplayName("Deve atualizar documento existente para EM_CONFERENCIA quando já cadastrado")
    void deveAtualizarDocumentoExistente() {
        Documento docExistente = Documento.criarRecebido(
                documentoId,
                tenantId,
                null,
                null,
                "bucket",
                "key",
                "nota.pdf",
                "application/pdf",
                1000L
        );
        when(documentoRepository.buscarPorId(documentoId)).thenReturn(Optional.of(docExistente));
        when(documentoRepository.salvar(any(Documento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProcessarDocumentoExtraidoUseCase.ProcessarDocumentoExtraidoCommand command =
                new ProcessarDocumentoExtraidoUseCase.ProcessarDocumentoExtraidoCommand(
                        tenantId,
                        documentoId,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        extracao,
                        boxes
                );

        Documento resultado = service.processar(command);

        assertEquals(StatusDocumento.EM_CONFERENCIA, resultado.getStatus());
        assertEquals("000123", resultado.getExtracaoSugerida().numeroDocumento());

        ArgumentCaptor<Documento> captor = ArgumentCaptor.forClass(Documento.class);
        verify(documentoRepository).salvar(captor.capture());
        assertEquals(StatusDocumento.EM_CONFERENCIA, captor.getValue().getStatus());
    }

    @Test
    @DisplayName("Deve retornar documento existente com idempotência e sem lançar exceção quando já estiver finalizado")
    void deveRetornarIdempotenteQuandoDocumentoJaFinalizado() {
        Documento docFinalizado = new Documento(
                documentoId,
                tenantId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                null,
                "bucket",
                "key",
                "arquivo.pdf",
                "application/pdf",
                1024L,
                StatusDocumento.PRONTO_PARA_TRANSFEREGOV,
                extracao,
                null,
                null,
                null,
                null,
                null
        );

        when(documentoRepository.buscarPorId(documentoId)).thenReturn(Optional.of(docFinalizado));

        ProcessarDocumentoExtraidoUseCase.ProcessarDocumentoExtraidoCommand command =
                new ProcessarDocumentoExtraidoUseCase.ProcessarDocumentoExtraidoCommand(
                        tenantId,
                        documentoId,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        extracao,
                        boxes
                );

        Documento resultado = service.processar(command);

        assertEquals(StatusDocumento.PRONTO_PARA_TRANSFEREGOV, resultado.getStatus());
        verify(documentoRepository, never()).salvar(any(Documento.class));
    }
}
