package br.com.govflow.core.domain.model;

import br.com.govflow.core.domain.exception.DocumentoEstadoInvalidoException;
import br.com.govflow.core.domain.exception.JustificativaObrigatoriaException;
import br.com.govflow.core.domain.model.documento.CategoriaDocumento;
import br.com.govflow.core.domain.model.documento.DocumentoHabilDados;
import br.com.govflow.core.domain.model.documento.FaseCicloVida;
import br.com.govflow.core.domain.model.documento.OrigemCanal;

import java.time.Instant;
import java.util.*;

/**
 * Agregado Raiz Universal do Documento no Ficheiro Digital do GovFlow.
 * Modelo de domínio puro sem dependências ou anotações de infraestrutura (JPA/Jackson).
 */
public class Documento {

    private final UUID id;
    private final UUID tenantId;
    private UUID prefeituraId;
    private UUID convenioId;
    private UUID contratoId;
    private UUID medicaoId;

    // Metadados do Ficheiro Digital
    private FaseCicloVida faseCicloVida;
    private CategoriaDocumento categoriaDocumento;
    private String pastaVirtual;
    private OrigemCanal origemCanal;
    private String hashSha256;
    private List<String> tags;
    private Map<String, Object> metadadosJson;
    private UUID criadoPorUsuarioId;

    // Armazenamento físico
    private ArmazenamentoArquivo armazenamento;
    private StatusDocumento status;

    // Dados de Processamento / Inteligência Artificial
    private ExtracaoSugerida extracaoSugerida;
    private BoundingBoxesData boundingBoxes;
    private DadosRevisaoAnalista dadosRevisao;
    private DocumentoHabilDados dadosHabeis;
    private String motivoRejeicao;

    private final Instant createdAt;
    private Instant updatedAt;

    public Documento(UUID id,
                     UUID tenantId,
                     UUID prefeituraId,
                     UUID convenioId,
                     UUID contratoId,
                     UUID medicaoId,
                     FaseCicloVida faseCicloVida,
                     CategoriaDocumento categoriaDocumento,
                     String pastaVirtual,
                     OrigemCanal origemCanal,
                     String hashSha256,
                     List<String> tags,
                     Map<String, Object> metadadosJson,
                     UUID criadoPorUsuarioId,
                     ArmazenamentoArquivo armazenamento,
                     StatusDocumento status,
                     ExtracaoSugerida extracaoSugerida,
                     BoundingBoxesData boundingBoxes,
                     DadosRevisaoAnalista dadosRevisao,
                     DocumentoHabilDados dadosHabeis,
                     String motivoRejeicao,
                     Instant createdAt,
                     Instant updatedAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = Objects.requireNonNull(tenantId, "TenantId é obrigatório para Documento.");
        this.prefeituraId = prefeituraId;
        this.convenioId = convenioId;
        this.contratoId = contratoId;
        this.medicaoId = medicaoId;
        this.faseCicloVida = faseCicloVida != null ? faseCicloVida : FaseCicloVida.FASE_05_EXECUCAO_FINANCEIRA;
        this.categoriaDocumento = categoriaDocumento != null ? categoriaDocumento : CategoriaDocumento.DOCUMENTO_HABIL;
        this.pastaVirtual = (pastaVirtual != null && !pastaVirtual.isBlank()) ? pastaVirtual : "/";
        this.origemCanal = origemCanal != null ? origemCanal : OrigemCanal.UPLOAD_MANUAL;
        this.hashSha256 = hashSha256;
        this.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>();
        this.metadadosJson = metadadosJson != null ? new HashMap<>(metadadosJson) : new HashMap<>();
        this.criadoPorUsuarioId = criadoPorUsuarioId;
        this.armazenamento = armazenamento;
        this.status = status != null ? status : StatusDocumento.RECEBIDO;
        this.extracaoSugerida = extracaoSugerida;
        this.boundingBoxes = boundingBoxes != null ? boundingBoxes : BoundingBoxesData.empty();
        this.dadosRevisao = dadosRevisao;
        this.dadosHabeis = dadosHabeis;
        this.motivoRejeicao = motivoRejeicao;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
    }

    /**
     * Construtor de compatibilidade para código pré-existente com ArmazenamentoArquivo.
     */
    public Documento(UUID id,
                     UUID tenantId,
                     UUID prefeituraId,
                     UUID convenioId,
                     UUID contratoId,
                     UUID medicaoId,
                     ArmazenamentoArquivo armazenamento,
                     StatusDocumento status,
                     ExtracaoSugerida extracaoSugerida,
                     BoundingBoxesData boundingBoxes,
                     DadosRevisaoAnalista dadosRevisao,
                     String motivoRejeicao,
                     Instant createdAt,
                     Instant updatedAt) {
        this(
                id,
                tenantId,
                prefeituraId,
                convenioId,
                contratoId,
                medicaoId,
                FaseCicloVida.FASE_05_EXECUCAO_FINANCEIRA,
                CategoriaDocumento.DOCUMENTO_HABIL,
                "/",
                OrigemCanal.UPLOAD_MANUAL,
                null,
                Collections.emptyList(),
                Collections.emptyMap(),
                null,
                armazenamento,
                status,
                extracaoSugerida,
                boundingBoxes,
                dadosRevisao,
                null,
                motivoRejeicao,
                createdAt,
                updatedAt
        );
    }

    /**
     * Construtor de compatibilidade com propriedades de S3 explícitas.
     */
    public Documento(UUID id,
                     UUID tenantId,
                     UUID prefeituraId,
                     UUID convenioId,
                     UUID contratoId,
                     UUID medicaoId,
                     String s3Bucket,
                     String s3Key,
                     String nomeArquivoOriginal,
                     String contentType,
                     Long tamanhoBytes,
                     StatusDocumento status,
                     ExtracaoSugerida extracaoSugerida,
                     BoundingBoxesData boundingBoxes,
                     DadosRevisaoAnalista dadosRevisao,
                     String motivoRejeicao,
                     Instant createdAt,
                     Instant updatedAt) {
        this(
                id,
                tenantId,
                prefeituraId,
                convenioId,
                contratoId,
                medicaoId,
                new ArmazenamentoArquivo(s3Bucket, s3Key, nomeArquivoOriginal, contentType, tamanhoBytes),
                status,
                extracaoSugerida,
                boundingBoxes,
                dadosRevisao,
                motivoRejeicao,
                createdAt,
                updatedAt
        );
    }

    /**
     * Construtor legado com boundingBoxes em Map.
     */
    public Documento(UUID id,
                     UUID tenantId,
                     UUID prefeituraId,
                     UUID convenioId,
                     String s3Bucket,
                     String s3Key,
                     String nomeArquivoOriginal,
                     String contentType,
                     Long tamanhoBytes,
                     StatusDocumento status,
                     ExtracaoSugerida extracaoSugerida,
                     Map<String, BoundingBox> boundingBoxes,
                     DadosRevisaoAnalista dadosRevisao,
                     String motivoRejeicao,
                     Instant createdAt,
                     Instant updatedAt) {
        this(
                id,
                tenantId,
                prefeituraId,
                convenioId,
                null,
                null,
                new ArmazenamentoArquivo(s3Bucket, s3Key, nomeArquivoOriginal, contentType, tamanhoBytes),
                status,
                extracaoSugerida,
                BoundingBoxesData.of(boundingBoxes),
                dadosRevisao,
                motivoRejeicao,
                createdAt,
                updatedAt
        );
    }

    public static Documento criarRecebido(UUID id,
                                          UUID tenantId,
                                          UUID prefeituraId,
                                          UUID convenioId,
                                          ArmazenamentoArquivo armazenamento) {
        return new Documento(
                id != null ? id : UUID.randomUUID(),
                tenantId,
                prefeituraId,
                convenioId,
                null,
                null,
                FaseCicloVida.FASE_05_EXECUCAO_FINANCEIRA,
                CategoriaDocumento.DOCUMENTO_HABIL,
                "/",
                OrigemCanal.UPLOAD_MANUAL,
                null,
                Collections.emptyList(),
                Collections.emptyMap(),
                null,
                armazenamento,
                StatusDocumento.RECEBIDO,
                null,
                BoundingBoxesData.empty(),
                null,
                null,
                null,
                Instant.now(),
                Instant.now()
        );
    }

    public static Documento criarRecebido(UUID id,
                                          UUID tenantId,
                                          UUID prefeituraId,
                                          UUID convenioId,
                                          String s3Bucket,
                                          String s3Key,
                                          String nomeArquivoOriginal,
                                          String contentType,
                                          Long tamanhoBytes) {
        return criarRecebido(
                id,
                tenantId,
                prefeituraId,
                convenioId,
                new ArmazenamentoArquivo(s3Bucket, s3Key, nomeArquivoOriginal, contentType, tamanhoBytes)
        );
    }

    public static Documento criarNovo(UUID id,
                                      UUID tenantId,
                                      UUID prefeituraId,
                                      UUID convenioId,
                                      FaseCicloVida faseCicloVida,
                                      CategoriaDocumento categoriaDocumento,
                                      String pastaVirtual,
                                      ArmazenamentoArquivo armazenamento,
                                      String hashSha256,
                                      OrigemCanal origemCanal,
                                      List<String> tags,
                                      Map<String, Object> metadadosJson,
                                      UUID criadoPorUsuarioId) {
        return new Documento(
                id != null ? id : UUID.randomUUID(),
                tenantId,
                prefeituraId,
                convenioId,
                null,
                null,
                faseCicloVida != null ? faseCicloVida : FaseCicloVida.FASE_05_EXECUCAO_FINANCEIRA,
                categoriaDocumento != null ? categoriaDocumento : CategoriaDocumento.DOCUMENTO_HABIL,
                pastaVirtual != null ? pastaVirtual : "/",
                origemCanal != null ? origemCanal : OrigemCanal.UPLOAD_MANUAL,
                hashSha256,
                tags,
                metadadosJson,
                criadoPorUsuarioId,
                armazenamento,
                StatusDocumento.RECEBIDO,
                null,
                BoundingBoxesData.empty(),
                null,
                null,
                null,
                Instant.now(),
                Instant.now()
        );
    }

    public void moverPasta(FaseCicloVida novaFase, String novaPastaVirtual) {
        if (novaFase != null) {
            this.faseCicloVida = novaFase;
        }
        if (novaPastaVirtual != null && !novaPastaVirtual.isBlank()) {
            this.pastaVirtual = novaPastaVirtual;
        }
        this.updatedAt = Instant.now();
    }

    public void atualizarClassificacao(FaseCicloVida novaFase, CategoriaDocumento novaCategoria, String novaPastaVirtual) {
        if (novaFase != null) {
            this.faseCicloVida = novaFase;
        }
        if (novaCategoria != null) {
            this.categoriaDocumento = novaCategoria;
        }
        if (novaPastaVirtual != null && !novaPastaVirtual.isBlank()) {
            this.pastaVirtual = novaPastaVirtual;
        }
        this.updatedAt = Instant.now();
    }

    public void atualizarArmazenamento(ArmazenamentoArquivo novoArmazenamento) {
        if (novoArmazenamento != null) {
            this.armazenamento = novoArmazenamento;
            this.updatedAt = Instant.now();
        }
    }

    public void marcarExcluido(String motivo) {
        this.status = StatusDocumento.EXCLUIDO;
        this.motivoRejeicao = motivo;
        this.updatedAt = Instant.now();
    }

    public void iniciarAnaliseIA() {
        this.status = StatusDocumento.EM_ANALISE_IA;
        this.updatedAt = Instant.now();
    }

    public boolean isEmAnalise() {
        return this.status == StatusDocumento.RECEBIDO || this.status == StatusDocumento.EM_ANALISE_IA;
    }

    public void vincularDadosHabeis(DocumentoHabilDados dadosHabeis) {
        this.dadosHabeis = dadosHabeis;
        this.updatedAt = Instant.now();
    }

    public void vincularContextoOperacional(UUID prefeituraId, UUID convenioId) {
        boolean alterado = false;
        if (prefeituraId != null && this.prefeituraId == null) {
            this.prefeituraId = prefeituraId;
            alterado = true;
        }
        if (convenioId != null && this.convenioId == null) {
            this.convenioId = convenioId;
            alterado = true;
        }
        if (alterado) {
            this.updatedAt = Instant.now();
        }
    }

    public boolean isFinalizado() {
        return this.status == StatusDocumento.PRONTO_PARA_TRANSFEREGOV
                || this.status == StatusDocumento.APROVADO
                || this.status == StatusDocumento.REJEITADO
                || this.status == StatusDocumento.EXCLUIDO;
    }

    public void registrarExtracaoIA(ExtracaoSugerida extracao, BoundingBoxesData boundingBoxes) {
        if (this.status == StatusDocumento.PRONTO_PARA_TRANSFEREGOV || this.status == StatusDocumento.REJEITADO || this.status == StatusDocumento.EXCLUIDO) {
            throw new DocumentoEstadoInvalidoException(this.status, "Registrar Extração da IA");
        }
        this.extracaoSugerida = extracao;
        this.boundingBoxes = boundingBoxes != null ? boundingBoxes : BoundingBoxesData.empty();
        this.status = StatusDocumento.EM_CONFERENCIA;
        this.updatedAt = Instant.now();
    }

    public void registrarExtracaoIA(ExtracaoSugerida extracao, Map<String, BoundingBox> boundingBoxes) {
        registrarExtracaoIA(extracao, BoundingBoxesData.of(boundingBoxes));
    }

    public AuditoriaRevisao aprovar(UUID analistaId, DadosRevisaoAnalista revisao, String observacao) {
        Objects.requireNonNull(analistaId, "AnalistaId é obrigatório para aprovação.");
        Objects.requireNonNull(revisao, "Dados de revisão são obrigatórios para aprovação.");

        if (this.status != StatusDocumento.EM_CONFERENCIA) {
            throw new DocumentoEstadoInvalidoException(this.status, "Aprovação do Documento");
        }

        revisao.validarConsistencia();

        DiffRevisao diff = DiffRevisao.comparar(this.extracaoSugerida, revisao);
        Map<String, Object> snapshotOriginal = this.extracaoSugerida != null ? this.extracaoSugerida.gerarSnapshot() : Collections.emptyMap();
        Map<String, Object> snapshotRevisado = revisao.gerarSnapshot();

        AuditoriaRevisao auditoria = AuditoriaRevisao.criarAprovacao(
                this.tenantId,
                this.id,
                analistaId,
                observacao,
                diff,
                snapshotOriginal,
                snapshotRevisado
        );

        this.dadosRevisao = revisao;
        this.status = StatusDocumento.PRONTO_PARA_TRANSFEREGOV;
        this.updatedAt = Instant.now();

        return auditoria;
    }

    public AuditoriaRevisao rejeitar(UUID analistaId, String motivo) {
        Objects.requireNonNull(analistaId, "AnalistaId é obrigatório para rejeição.");

        if (this.status != StatusDocumento.EM_CONFERENCIA) {
            throw new DocumentoEstadoInvalidoException(this.status, "Rejeição do Documento");
        }

        if (motivo == null || motivo.trim().isEmpty()) {
            throw new JustificativaObrigatoriaException("Rejeição de Documento");
        }

        Map<String, Object> snapshotOriginal = this.extracaoSugerida != null ? this.extracaoSugerida.gerarSnapshot() : Collections.emptyMap();

        AuditoriaRevisao auditoria = AuditoriaRevisao.criarRejeicao(
                this.tenantId,
                this.id,
                analistaId,
                motivo.trim(),
                snapshotOriginal
        );

        this.motivoRejeicao = motivo.trim();
        this.status = StatusDocumento.REJEITADO;
        this.updatedAt = Instant.now();

        return auditoria;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getPrefeituraId() {
        return prefeituraId;
    }

    public void setPrefeituraId(UUID prefeituraId) {
        this.prefeituraId = prefeituraId;
        this.updatedAt = Instant.now();
    }

    public UUID getConvenioId() {
        return convenioId;
    }

    public void setConvenioId(UUID convenioId) {
        this.convenioId = convenioId;
        this.updatedAt = Instant.now();
    }

    public UUID getContratoId() {
        return contratoId;
    }

    public void setContratoId(UUID contratoId) {
        this.contratoId = contratoId;
        this.updatedAt = Instant.now();
    }

    public UUID getMedicaoId() {
        return medicaoId;
    }

    public void setMedicaoId(UUID medicaoId) {
        this.medicaoId = medicaoId;
        this.updatedAt = Instant.now();
    }

    public FaseCicloVida getFaseCicloVida() {
        return faseCicloVida;
    }

    public void setFaseCicloVida(FaseCicloVida faseCicloVida) {
        this.faseCicloVida = faseCicloVida;
        this.updatedAt = Instant.now();
    }

    public CategoriaDocumento getCategoriaDocumento() {
        return categoriaDocumento;
    }

    public void setCategoriaDocumento(CategoriaDocumento categoriaDocumento) {
        this.categoriaDocumento = categoriaDocumento;
        this.updatedAt = Instant.now();
    }

    public String getPastaVirtual() {
        return pastaVirtual;
    }

    public void setPastaVirtual(String pastaVirtual) {
        this.pastaVirtual = pastaVirtual;
        this.updatedAt = Instant.now();
    }

    public OrigemCanal getOrigemCanal() {
        return origemCanal;
    }

    public void setOrigemCanal(OrigemCanal origemCanal) {
        this.origemCanal = origemCanal;
        this.updatedAt = Instant.now();
    }

    public String getHashSha256() {
        return hashSha256;
    }

    public void setHashSha256(String hashSha256) {
        this.hashSha256 = hashSha256;
        this.updatedAt = Instant.now();
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>();
        this.updatedAt = Instant.now();
    }

    public Map<String, Object> getMetadadosJson() {
        return metadadosJson;
    }

    public void setMetadadosJson(Map<String, Object> metadadosJson) {
        this.metadadosJson = metadadosJson != null ? new HashMap<>(metadadosJson) : new HashMap<>();
        this.updatedAt = Instant.now();
    }

    public UUID getCriadoPorUsuarioId() {
        return criadoPorUsuarioId;
    }

    public void setCriadoPorUsuarioId(UUID criadoPorUsuarioId) {
        this.criadoPorUsuarioId = criadoPorUsuarioId;
        this.updatedAt = Instant.now();
    }

    public ArmazenamentoArquivo getArmazenamento() {
        return armazenamento;
    }

    public void setArmazenamento(ArmazenamentoArquivo armazenamento) {
        this.armazenamento = armazenamento;
        this.updatedAt = Instant.now();
    }

    public String getS3Bucket() {
        return armazenamento != null ? armazenamento.s3Bucket() : null;
    }

    public String getS3Key() {
        return armazenamento != null ? armazenamento.s3Key() : null;
    }

    public String getNomeArquivoOriginal() {
        return armazenamento != null ? armazenamento.nomeArquivoOriginal() : null;
    }

    public String getContentType() {
        return armazenamento != null ? armazenamento.contentType() : null;
    }

    public Long getTamanhoBytes() {
        return armazenamento != null ? armazenamento.tamanhoBytes() : null;
    }

    public StatusDocumento getStatus() {
        return status;
    }

    public void setStatus(StatusDocumento status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public ExtracaoSugerida getExtracaoSugerida() {
        return extracaoSugerida;
    }

    public BoundingBoxesData getBoundingBoxesData() {
        return boundingBoxes;
    }

    public Map<String, BoundingBox> getBoundingBoxes() {
        return boundingBoxes != null ? boundingBoxes.toMap() : Collections.emptyMap();
    }

    public DadosRevisaoAnalista getDadosRevisao() {
        return dadosRevisao;
    }

    public DocumentoHabilDados getDadosHabeis() {
        return dadosHabeis;
    }

    public String getMotivoRejeicao() {
        return motivoRejeicao;
    }

    public void setMotivoRejeicao(String motivoRejeicao) {
        this.motivoRejeicao = motivoRejeicao;
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Documento documento = (Documento) o;
        return Objects.equals(id, documento.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
