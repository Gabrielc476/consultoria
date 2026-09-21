package br.com.govflow.core.application.service;

import br.com.govflow.core.application.port.in.ObterArquivoDocumentoUseCase;
import br.com.govflow.core.application.port.out.DocumentoRepositoryPort;
import br.com.govflow.core.application.port.out.DocumentoStoragePort;
import br.com.govflow.core.domain.exception.DocumentoNaoEncontradoException;
import br.com.govflow.core.domain.model.Documento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObterArquivoDocumentoServiceTest {

    @Mock
    private DocumentoRepositoryPort documentoRepositoryPort;

    @Mock
    private DocumentoStoragePort documentoStoragePort;

    @InjectMocks
    private ObterArquivoDocumentoService service;

    private UUID documentoId;
    private Documento documento;

    @BeforeEach
    void setUp() {
        documentoId = UUID.randomUUID();
        documento = new Documento(
                documentoId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                null,
                null,
                "govflow-documents",
                "raw/teste/arquivo.pdf",
                "arquivo.pdf",
                "application/pdf",
                1024L,
                br.com.govflow.core.domain.model.StatusDocumento.RECEBIDO,
                null,
                null,
                null,
                null,
                java.time.Instant.now(),
                java.time.Instant.now()
        );
    }

    @Test
    @DisplayName("Deve carregar arquivo original do storage quando disponível")
    void deveCarregarArquivoOriginalDoStorage() throws Exception {
        byte[] bytes = "%PDF-1.4 conteudo".getBytes(StandardCharsets.UTF_8);
        when(documentoRepositoryPort.buscarPorId(documentoId)).thenReturn(Optional.of(documento));
        when(documentoStoragePort.carregarArquivo("govflow-documents", "raw/teste/arquivo.pdf"))
                .thenReturn(Optional.of(new ByteArrayInputStream(bytes)));

        ObterArquivoDocumentoUseCase.ArquivoConteudo resultado = service.obterArquivo(documentoId);

        assertThat(resultado).isNotNull();
        assertThat(resultado.contentType()).isEqualTo("application/pdf");
        assertThat(resultado.nomeArquivoOriginal()).isEqualTo("arquivo.pdf");
        assertThat(resultado.inputStream().readAllBytes()).isEqualTo(bytes);
    }

    @Test
    @DisplayName("Deve gerar fallback em memória quando storage não encontrar o arquivo")
    void deveGerarFallbackQuandoStorageVazio() throws Exception {
        when(documentoRepositoryPort.buscarPorId(documentoId)).thenReturn(Optional.of(documento));
        when(documentoStoragePort.carregarArquivo("govflow-documents", "raw/teste/arquivo.pdf"))
                .thenReturn(Optional.empty());

        ObterArquivoDocumentoUseCase.ArquivoConteudo resultado = service.obterArquivo(documentoId);

        assertThat(resultado).isNotNull();
        assertThat(resultado.contentType()).isEqualTo("application/pdf");
        assertThat(resultado.tamanhoBytes()).isGreaterThan(0);
        byte[] lidos = resultado.inputStream().readAllBytes();
        assertThat(new String(lidos, StandardCharsets.ISO_8859_1)).contains("%PDF-1.4");
    }

    @Test
    @DisplayName("Deve lançar DocumentoNaoEncontradoException se o documento não existir")
    void deveLancarExcecaoQuandoDocumentoNaoExistir() {
        when(documentoRepositoryPort.buscarPorId(documentoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obterArquivo(documentoId))
                .isInstanceOf(DocumentoNaoEncontradoException.class);
    }
}
