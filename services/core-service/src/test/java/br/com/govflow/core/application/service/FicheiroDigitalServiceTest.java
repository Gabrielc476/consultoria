package br.com.govflow.core.application.service;

import br.com.govflow.core.application.port.in.GerenciarFicheiroDigitalUseCase;
import br.com.govflow.core.application.port.out.ConvenioRepositoryPort;
import br.com.govflow.core.application.port.out.DocumentoRepositoryPort;
import br.com.govflow.core.application.port.out.DocumentoStoragePort;
import br.com.govflow.core.application.port.out.FicheiroRepositoryPort;
import br.com.govflow.core.application.port.out.UsuarioRepositoryPort;
import br.com.govflow.core.domain.exception.AcessoNegadoException;
import br.com.govflow.core.domain.model.ArmazenamentoArquivo;
import br.com.govflow.core.domain.model.Documento;
import br.com.govflow.core.domain.model.convenio.Convenio;
import br.com.govflow.core.domain.model.documento.CategoriaDocumento;
import br.com.govflow.core.domain.model.documento.FaseCicloVida;
import br.com.govflow.core.domain.model.documento.FicheiroDigital;
import br.com.govflow.core.domain.model.documento.OrigemCanal;
import br.com.govflow.core.infrastructure.interceptor.TenantContext;
import br.com.govflow.core.infrastructure.interceptor.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Serviço: FicheiroDigitalService (Regras de Negócio e Escopo Multi-Tenant)")
class FicheiroDigitalServiceTest {

    @Mock
    private ConvenioRepositoryPort convenioRepository;

    @Mock
    private DocumentoRepositoryPort documentoRepository;

    @Mock
    private FicheiroRepositoryPort ficheiroRepository;

    @Mock
    private DocumentoStoragePort storagePort;

    @Mock
    private UsuarioRepositoryPort usuarioRepository;

    @InjectMocks
    private FicheiroDigitalService service;

    private UUID tenantId;
    private UUID prefeituraId;
    private UUID convenioId;
    private Convenio convenio;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        prefeituraId = UUID.randomUUID();
        convenioId = UUID.randomUUID();

        TenantContext.setCurrentTenant(tenantId);
        UserContext.setCurrentUser(UUID.randomUUID(), tenantId, Set.of("ADMIN"), Collections.emptySet());

        convenio = new Convenio(
                convenioId, tenantId, prefeituraId,
                "954120/2024", "PROC-001", "MEC", "Creche Proinfancia",
                new BigDecimal("500000.00"), new BigDecimal("450000.00"), new BigDecimal("50000.00"),
                "EM_EXECUCAO", false, null, LocalDate.now(), LocalDate.now().plusYears(1),
                false, null, null, null, null, null, null
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        UserContext.clear();
    }

    @Test
    @DisplayName("Admin deve obter Ficheiro Digital com sucesso")
    void adminDeveObterFicheiroComSucesso() {
        when(convenioRepository.buscarPorId(convenioId)).thenReturn(Optional.of(convenio));
        FicheiroDigital ficheiroEsperado = new FicheiroDigital(convenioId, prefeituraId, tenantId, "954120/2024", "Creche", Collections.emptyList());
        when(ficheiroRepository.carregarFicheiroDigital(convenioId)).thenReturn(ficheiroEsperado);

        FicheiroDigital resultado = service.obterFicheiro(convenioId);

        assertNotNull(resultado);
        assertEquals(convenioId, resultado.getConvenioId());
        verify(ficheiroRepository).carregarFicheiroDigital(convenioId);
    }

    @Test
    @DisplayName("Agente sem acesso à prefeitura deve receber AcessoNegadoException (403)")
    void agenteSemAcessoDeveLancarAcessoNegado() {
        UUID agenteId = UUID.randomUUID();
        UUID outraPrefId = UUID.randomUUID();
        UserContext.setCurrentUser(agenteId, tenantId, Set.of("AGENTE"), Set.of(outraPrefId));

        when(convenioRepository.buscarPorId(convenioId)).thenReturn(Optional.of(convenio));

        assertThrows(AcessoNegadoException.class, () -> service.obterFicheiro(convenioId));
        verify(ficheiroRepository, never()).carregarFicheiroDigital(any());
    }

    @Test
    @DisplayName("Upload de documento deve salvar no storage, calcular SHA-256 e registrar auditoria")
    void uploadDocumentoDeveSalvarERegistrarAuditoria() {
        when(convenioRepository.buscarPorId(convenioId)).thenReturn(Optional.of(convenio));
        when(documentoRepository.salvar(any(Documento.class))).thenAnswer(inv -> inv.getArgument(0));

        byte[] conteudo = "CONTEUDO_PDF_DE_TESTE".getBytes();
        GerenciarFicheiroDigitalUseCase.UploadDocumentoCommand command =
                new GerenciarFicheiroDigitalUseCase.UploadDocumentoCommand(
                        convenioId,
                        FaseCicloVida.FASE_02_CLAUSULA_SUSPENSIVA,
                        CategoriaDocumento.LICENCA_AMBIENTAL,
                        "/02_Clausula_Suspensiva_e_Engenharia/Licencas",
                        "Licenca_Previa.pdf",
                        "application/pdf",
                        conteudo,
                        List.of("ambiental", "lp"),
                        UserContext.getUserId()
                );

        Documento salvo = service.uploadDocumento(command);

        assertNotNull(salvo);
        assertEquals(FaseCicloVida.FASE_02_CLAUSULA_SUSPENSIVA, salvo.getFaseCicloVida());
        assertEquals(CategoriaDocumento.LICENCA_AMBIENTAL, salvo.getCategoriaDocumento());
        assertEquals("Licenca_Previa.pdf", salvo.getNomeArquivoOriginal());
        assertNotNull(salvo.getHashSha256());

        verify(storagePort).salvarArquivo(isNull(), anyString(), any(InputStream.class), eq((long) conteudo.length), eq("application/pdf"));
        verify(documentoRepository).salvar(any(Documento.class));
        verify(documentoRepository).salvarAuditoria(any());
    }

    @Test
    @DisplayName("Mover documento deve atualizar fase/pasta e registrar auditoria")
    void moverDocumentoDeveAtualizarPastaEAuditar() {
        UUID docId = UUID.randomUUID();
        Documento doc = Documento.criarNovo(
                docId, tenantId, prefeituraId, convenioId,
                FaseCicloVida.FASE_05_EXECUCAO_FINANCEIRA,
                CategoriaDocumento.DOCUMENTO_HABIL,
                "/",
                new ArmazenamentoArquivo("bucket", "key", "doc.pdf", "application/pdf", 100L),
                "hash",
                OrigemCanal.UPLOAD_MANUAL,
                null,
                null,
                null
        );

        when(documentoRepository.buscarPorId(docId)).thenReturn(Optional.of(doc));
        when(documentoRepository.salvar(any(Documento.class))).thenAnswer(inv -> inv.getArgument(0));

        GerenciarFicheiroDigitalUseCase.MoverDocumentoCommand command =
                new GerenciarFicheiroDigitalUseCase.MoverDocumentoCommand(
                        docId,
                        FaseCicloVida.FASE_02_CLAUSULA_SUSPENSIVA,
                        "/02_Clausula_Suspensiva_e_Engenharia/Projetos",
                        "Reclassificado para a fase correta",
                        UserContext.getUserId()
                );

        Documento movido = service.moverDocumento(command);

        assertEquals(FaseCicloVida.FASE_02_CLAUSULA_SUSPENSIVA, movido.getFaseCicloVida());
        assertEquals("/02_Clausula_Suspensiva_e_Engenharia/Projetos", movido.getPastaVirtual());
        verify(documentoRepository).salvarAuditoria(any());
    }

    @Test
    @DisplayName("Excluir documento deve marcar soft delete e registrar auditoria")
    void excluirDocumentoDeveMarcarSoftDelete() {
        UUID docId = UUID.randomUUID();
        Documento doc = Documento.criarNovo(
                docId, tenantId, prefeituraId, convenioId,
                FaseCicloVida.FASE_05_EXECUCAO_FINANCEIRA,
                CategoriaDocumento.DOCUMENTO_HABIL,
                "/",
                new ArmazenamentoArquivo("bucket", "key", "doc.pdf", "application/pdf", 100L),
                "hash",
                OrigemCanal.UPLOAD_MANUAL,
                null,
                null,
                null
        );

        when(documentoRepository.buscarPorId(docId)).thenReturn(Optional.of(doc));
        when(documentoRepository.salvar(any(Documento.class))).thenAnswer(inv -> inv.getArgument(0));

        GerenciarFicheiroDigitalUseCase.ExcluirDocumentoCommand command =
                new GerenciarFicheiroDigitalUseCase.ExcluirDocumentoCommand(docId, "Arquivo duplicado", UserContext.getUserId());

        service.excluirDocumento(command);

        assertTrue(doc.isFinalizado());
        verify(documentoRepository).salvar(doc);
        verify(documentoRepository).salvarAuditoria(any());
    }

    @Test
    @DisplayName("Obter URL preview deve delegar para DocumentoStoragePort com 15 minutos de expiração")
    void obterUrlPreviewDeveRetornarUrlAssinada() {
        UUID docId = UUID.randomUUID();
        Documento doc = Documento.criarNovo(
                docId, tenantId, prefeituraId, convenioId,
                FaseCicloVida.FASE_02_CLAUSULA_SUSPENSIVA,
                CategoriaDocumento.PROJETO_ENGENHARIA,
                "/",
                new ArmazenamentoArquivo("govflow-documentos", "tenants/.../projeto.pdf", "projeto.pdf", "application/pdf", 100L),
                "hash",
                OrigemCanal.UPLOAD_MANUAL,
                null,
                null,
                null
        );

        when(documentoRepository.buscarPorId(docId)).thenReturn(Optional.of(doc));
        when(storagePort.gerarPresignedUrlPreview("govflow-documentos", "tenants/.../projeto.pdf", 15))
                .thenReturn("http://localhost:9000/govflow-documentos/...presigned");

        String url = service.obterUrlPreview(docId);

        assertNotNull(url);
        assertTrue(url.contains("presigned"));
        verify(storagePort).gerarPresignedUrlPreview("govflow-documentos", "tenants/.../projeto.pdf", 15);
    }
}
