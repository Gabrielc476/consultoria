package br.com.govflow.core.application.service;

import br.com.govflow.core.application.port.out.ConvenioRepositoryPort;
import br.com.govflow.core.application.port.out.DocumentoRepositoryPort;
import br.com.govflow.core.application.port.out.DocumentoStoragePort;
import br.com.govflow.core.application.port.out.UsuarioRepositoryPort;
import br.com.govflow.core.domain.exception.AcessoNegadoException;
import br.com.govflow.core.domain.exception.ConvenioNaoEncontradoException;
import br.com.govflow.core.domain.model.ArmazenamentoArquivo;
import br.com.govflow.core.domain.model.Documento;
import br.com.govflow.core.domain.model.convenio.Convenio;
import br.com.govflow.core.domain.model.documento.CategoriaDocumento;
import br.com.govflow.core.domain.model.documento.FaseCicloVida;
import br.com.govflow.core.domain.model.documento.OrigemCanal;
import br.com.govflow.core.infrastructure.interceptor.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExportarFicheiroZipServiceTest {

    @Mock
    private ConvenioRepositoryPort convenioRepository;

    @Mock
    private DocumentoRepositoryPort documentoRepository;

    @Mock
    private DocumentoStoragePort storagePort;

    @Mock
    private UsuarioRepositoryPort usuarioRepository;

    @InjectMocks
    private ExportarFicheiroZipService exportarZipService;

    private UUID tenantId;
    private UUID prefeituraId;
    private UUID convenioId;
    private Convenio convenio;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        prefeituraId = UUID.randomUUID();
        convenioId = UUID.randomUUID();

        convenio = new Convenio(
                convenioId,
                tenantId,
                prefeituraId,
                "942100/2024",
                "00123/2024",
                "Ministério das Cidades",
                "Pavimentação Asfáltica e Drenagem",
                new BigDecimal("500000.00"),
                new BigDecimal("450000.00"),
                new BigDecimal("50000.00"),
                "EM_EXECUCAO",
                false,
                null,
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2025, 12, 31),
                false,
                null,
                null,
                "NAO_APLICA",
                null,
                Instant.now(),
                Instant.now()
        );
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    @DisplayName("Deve exportar convênio integral em ZIP organizando pelas pastas das 10 Fases e ignorando excluídos")
    void deveExportarConvenioIntegralEmZip() throws Exception {
        UUID doc1Id = UUID.randomUUID();
        Documento doc1 = Documento.criarNovo(
                doc1Id,
                tenantId,
                prefeituraId,
                convenioId,
                FaseCicloVida.FASE_01_CELEBRACAO,
                CategoriaDocumento.PROPOSTA_PLANO_TRABALHO,
                "/01_Celebracao_e_Formalizacao",
                new ArmazenamentoArquivo("govflow-docs", "tenants/1/fase1/plano.pdf", "plano.pdf", "application/pdf", 100L),
                "hash1",
                OrigemCanal.UPLOAD_MANUAL,
                null,
                null,
                null
        );

        UUID doc2Id = UUID.randomUUID();
        Documento doc2 = Documento.criarNovo(
                doc2Id,
                tenantId,
                prefeituraId,
                convenioId,
                FaseCicloVida.FASE_05_EXECUCAO_FINANCEIRA,
                CategoriaDocumento.DOCUMENTO_HABIL,
                "/05_Execucao_Financeira_e_Pagamentos",
                new ArmazenamentoArquivo("govflow-docs", "tenants/1/fase5/nf_001.pdf", "nf_001.pdf", "application/pdf", 200L),
                "hash2",
                OrigemCanal.UPLOAD_MANUAL,
                null,
                null,
                null
        );

        UUID docExcluidoId = UUID.randomUUID();
        Documento docExcluido = Documento.criarNovo(
                docExcluidoId,
                tenantId,
                prefeituraId,
                convenioId,
                FaseCicloVida.FASE_05_EXECUCAO_FINANCEIRA,
                CategoriaDocumento.ORDEM_BANCARIA_OBTV,
                "/05_Execucao_Financeira_e_Pagamentos",
                new ArmazenamentoArquivo("govflow-docs", "tenants/1/fase5/del.pdf", "del.pdf", "application/pdf", 50L),
                "hash3",
                OrigemCanal.UPLOAD_MANUAL,
                null,
                null,
                null
        );
        docExcluido.marcarExcluido("Arquivo duplicado");

        when(convenioRepository.buscarPorId(convenioId)).thenReturn(Optional.of(convenio));
        when(documentoRepository.listarPorConvenioId(convenioId)).thenReturn(List.of(doc1, doc2, docExcluido));

        byte[] conteudoDoc1 = "Conteudo Plano de Trabalho".getBytes(StandardCharsets.UTF_8);
        byte[] conteudoDoc2 = "Conteudo Nota Fiscal".getBytes(StandardCharsets.UTF_8);

        when(storagePort.carregarArquivo("govflow-docs", "tenants/1/fase1/plano.pdf"))
                .thenReturn(Optional.of(new ByteArrayInputStream(conteudoDoc1)));
        when(storagePort.carregarArquivo("govflow-docs", "tenants/1/fase5/nf_001.pdf"))
                .thenReturn(Optional.of(new ByteArrayInputStream(conteudoDoc2)));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        exportarZipService.exportarConvenioIntegral(convenioId, baos);

        byte[] zipBytes = baos.toByteArray();
        assertThat(zipBytes).isNotEmpty();

        List<String> entryNames = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entryNames.add(entry.getName());
                zis.closeEntry();
            }
        }

        assertThat(entryNames).hasSize(2);
        assertThat(entryNames).contains(
                "SICONV_942100_2024/01_Celebracao_e_Formalizacao/plano.pdf",
                "SICONV_942100_2024/05_Execucao_Financeira_e_Pagamentos/nf_001.pdf"
        );
    }

    @Test
    @DisplayName("Deve barrar com AcessoNegadoException quando usuário AGENTE não tem permissão para a prefeitura")
    void deveLancarAcessoNegadoParaAgenteSemPermissao() {
        UUID userId = UUID.randomUUID();
        UserContext.setCurrentUser(userId, tenantId, Set.of("AGENTE"), Set.of(UUID.randomUUID()));

        when(convenioRepository.buscarPorId(convenioId)).thenReturn(Optional.of(convenio));
        when(usuarioRepository.buscarPrefeiturasAtribuidas(userId)).thenReturn(Set.of());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        assertThatThrownBy(() -> exportarZipService.exportarConvenioIntegral(convenioId, baos))
                .isInstanceOf(AcessoNegadoException.class)
                .hasMessageContaining("não possui autorização");

        verify(documentoRepository, never()).listarPorConvenioId(any());
    }

    @Test
    @DisplayName("Deve lançar ConvenioNaoEncontradoException quando convênio não existir")
    void deveLancarExceptionQuandoConvenioNaoExistir() {
        when(convenioRepository.buscarPorId(convenioId)).thenReturn(Optional.empty());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        assertThatThrownBy(() -> exportarZipService.exportarConvenioIntegral(convenioId, baos))
                .isInstanceOf(ConvenioNaoEncontradoException.class);
    }

    @Test
    @DisplayName("Deve exportar apenas documentos de uma fase específica")
    void deveExportarApenasFaseEspecifica() throws Exception {
        UUID docId = UUID.randomUUID();
        Documento doc = Documento.criarNovo(
                docId,
                tenantId,
                prefeituraId,
                convenioId,
                FaseCicloVida.FASE_02_CLAUSULA_SUSPENSIVA,
                CategoriaDocumento.PROJETO_ENGENHARIA,
                "/02_Clausula_Suspensiva",
                new ArmazenamentoArquivo("govflow-docs", "tenants/1/fase2/projeto.pdf", "projeto.pdf", "application/pdf", 100L),
                "hashProjeto",
                OrigemCanal.UPLOAD_MANUAL,
                null,
                null,
                null
        );

        when(convenioRepository.buscarPorId(convenioId)).thenReturn(Optional.of(convenio));
        when(documentoRepository.listarPorConvenioIdEFase(convenioId, FaseCicloVida.FASE_02_CLAUSULA_SUSPENSIVA))
                .thenReturn(List.of(doc));

        when(storagePort.carregarArquivo("govflow-docs", "tenants/1/fase2/projeto.pdf"))
                .thenReturn(Optional.of(new ByteArrayInputStream("Projeto basico".getBytes(StandardCharsets.UTF_8))));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        exportarZipService.exportarFaseEspecifica(convenioId, FaseCicloVida.FASE_02_CLAUSULA_SUSPENSIVA, baos);

        List<String> entryNames = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entryNames.add(entry.getName());
                zis.closeEntry();
            }
        }

        assertThat(entryNames).containsExactly("SICONV_942100_2024/02_Clausula_Suspensiva_e_Engenharia/projeto.pdf");
    }
}
