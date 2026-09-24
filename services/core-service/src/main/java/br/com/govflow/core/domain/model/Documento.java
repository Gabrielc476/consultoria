package br.com.govflow.core.domain.model;

import br.com.govflow.core.domain.exception.DocumentoEstadoInvalidoException;
import br.com.govflow.core.domain.exception.JustificativaObrigatoriaException;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class Documento {

    private final UUID id;
    private final UUID tenantId;
    private UUID prefeituraId;
    private UUID convenioId;
    private UUID contratoId;
    private UUID medicaoId;
    private ArmazenamentoArquivo armazenamento;
    private StatusDocumento status;
    private ExtracaoSugerida extracaoSugerida;
    private BoundingBoxesData boundingBoxes;
    private DadosRevisaoAnalista dadosRevisao;
    private String motivoRejeicao;
    private final Instant createdAt;
    private Instant updatedAt;

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
        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = Objects.requireNonNull(tenantId, "TenantId é obrigatório para Documento.");
        this.prefeituraId = prefeituraId;
        this.convenioId = convenioId;
        this.contratoId = contratoId;
        this.medicaoId = medicaoId;
        this.armazenamento = armazenamento;
        this.status = status != null ? status : StatusDocumento.RECEBIDO;
        this.extracaoSugerida = extracaoSugerida;
        this.boundingBoxes = boundingBoxes != null ? boundingBoxes : BoundingBoxesData.empty();
        this.dadosRevisao = dadosRevisao;
        this.motivoRejeicao = motivoRejeicao;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
    }

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
     * Construtor de compatibilidade para código pré-existente (sem contratoId/medicaoId explícitos).
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
                armazenamento,
                StatusDocumento.RECEBIDO,
                null,
                BoundingBoxesData.empty(),
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
        return this.status == StatusDocumento.PRONTO_PARA_TRANSFEREGOV || this.status == StatusDocumento.REJEITADO;
    }

    public void registrarExtracaoIA(ExtracaoSugerida extracao, BoundingBoxesData boundingBoxes) {
        if (this.status == StatusDocumento.PRONTO_PARA_TRANSFEREGOV || this.status == StatusDocumento.REJEITADO) {
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

    public ArmazenamentoArquivo getArmazenamento() {
        return armazenamento;
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

    public ExtracaoSugerida getExtracaoSugerida() {
        return extracaoSugerida;
    }

    public BoundingBoxesData getBoundingBoxesData() {
        return boundingBoxes;
    }

    public Map<String, BoundingBox> getBoundingBoxes() {
        return boundingBoxes.toMap();
    }

    public DadosRevisaoAnalista getDadosRevisao() {
        return dadosRevisao;
    }

    public String getMotivoRejeicao() {
        return motivoRejeicao;
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
