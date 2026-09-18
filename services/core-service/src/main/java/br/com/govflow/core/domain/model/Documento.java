package br.com.govflow.core.domain.model;

import br.com.govflow.core.domain.exception.CamposObrigatoriosAusentesException;
import br.com.govflow.core.domain.exception.DocumentoEstadoInvalidoException;
import br.com.govflow.core.domain.exception.InconsistenciaMatematicaException;
import br.com.govflow.core.domain.exception.JustificativaObrigatoriaException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class Documento {

    private static final BigDecimal TOLERANCIA_MATEMATICA = BigDecimal.ZERO;

    private final UUID id;
    private final UUID tenantId;
    private UUID prefeituraId;
    private UUID convenioId;
    private UUID contratoId;
    private UUID medicaoId;
    private String s3Bucket;
    private String s3Key;
    private String nomeArquivoOriginal;
    private String contentType;
    private Long tamanhoBytes;
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
        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = Objects.requireNonNull(tenantId, "TenantId é obrigatório para Documento.");
        this.prefeituraId = prefeituraId;
        this.convenioId = convenioId;
        this.contratoId = contratoId;
        this.medicaoId = medicaoId;
        this.s3Bucket = s3Bucket;
        this.s3Key = s3Key;
        this.nomeArquivoOriginal = nomeArquivoOriginal;
        this.contentType = contentType;
        this.tamanhoBytes = tamanhoBytes;
        this.status = status != null ? status : StatusDocumento.RECEBIDO;
        this.extracaoSugerida = extracaoSugerida;
        this.boundingBoxes = boundingBoxes != null ? boundingBoxes : BoundingBoxesData.empty();
        this.dadosRevisao = dadosRevisao;
        this.motivoRejeicao = motivoRejeicao;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
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
                s3Bucket,
                s3Key,
                nomeArquivoOriginal,
                contentType,
                tamanhoBytes,
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
                                          String s3Bucket,
                                          String s3Key,
                                          String nomeArquivoOriginal,
                                          String contentType,
                                          Long tamanhoBytes) {
        return new Documento(
                id != null ? id : UUID.randomUUID(),
                tenantId,
                prefeituraId,
                convenioId,
                null,
                null,
                s3Bucket,
                s3Key,
                nomeArquivoOriginal,
                contentType,
                tamanhoBytes,
                StatusDocumento.RECEBIDO,
                null,
                BoundingBoxesData.empty(),
                null,
                null,
                Instant.now(),
                Instant.now()
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

        validarCamposObrigatorios(revisao);
        validarConsistenciaMatematica(revisao);

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

    private void validarCamposObrigatorios(DadosRevisaoAnalista revisao) {
        List<String> faltantes = new ArrayList<>();

        if (revisao.tipoDocumento() == null) {
            faltantes.add("tipoDocumento");
        }
        if (revisao.numeroDocumento() == null || revisao.numeroDocumento().trim().isEmpty()) {
            faltantes.add("numeroDocumento");
        }
        if (revisao.dataEmissao() == null) {
            faltantes.add("dataEmissao");
        }
        if (revisao.cnpjCredor() == null || revisao.cnpjCredor().trim().isEmpty()) {
            faltantes.add("cnpjCredor");
        } else {
            try {
                new Cnpj(revisao.cnpjCredor());
            } catch (Exception e) {
                faltantes.add("cnpjCredor (CNPJ inválido: " + e.getMessage() + ")");
            }
        }
        if (revisao.razaoSocialCredor() == null || revisao.razaoSocialCredor().trim().isEmpty()) {
            faltantes.add("razaoSocialCredor");
        }
        if (revisao.valorBruto() == null || revisao.valorBruto().compareTo(BigDecimal.ZERO) <= 0) {
            faltantes.add("valorBruto (deve ser maior que zero)");
        }
        if (revisao.valorLiquido() == null || revisao.valorLiquido().compareTo(BigDecimal.ZERO) < 0) {
            faltantes.add("valorLiquido (não pode ser negativo)");
        }

        if (!faltantes.isEmpty()) {
            throw new CamposObrigatoriosAusentesException(faltantes);
        }
    }

    private void validarConsistenciaMatematica(DadosRevisaoAnalista revisao) {
        BigDecimal valorBruto = revisao.valorBruto();
        BigDecimal valorLiquido = revisao.valorLiquido();

        BigDecimal somaRetencoes = revisao.retencoes().stream()
                .map(RetencaoTributaria::valor)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDeducoes = revisao.valorTotalDeducoes();

        // Se retenções forem informadas, sua soma deve ser estritamente igual a valorTotalDeducoes
        if (!revisao.retencoes().isEmpty() && totalDeducoes != null && totalDeducoes.compareTo(BigDecimal.ZERO) > 0) {
            if (somaRetencoes.compareTo(totalDeducoes) != 0) {
                throw new InconsistenciaMatematicaException(
                        String.format("A soma das retenções tributárias discriminadas (%s) difere do valor total de deduções informado (%s).",
                                somaRetencoes, totalDeducoes)
                );
            }
        }

        if (totalDeducoes == null || totalDeducoes.compareTo(BigDecimal.ZERO) == 0) {
            totalDeducoes = somaRetencoes;
        }

        BigDecimal valorLiquidoEsperado = valorBruto.subtract(totalDeducoes);
        BigDecimal diferenca = valorLiquidoEsperado.subtract(valorLiquido).abs().setScale(2, RoundingMode.HALF_UP);

        if (diferenca.compareTo(TOLERANCIA_MATEMATICA) > 0) {
            throw new InconsistenciaMatematicaException(valorBruto, totalDeducoes, valorLiquido, diferenca);
        }
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

    public String getS3Bucket() {
        return s3Bucket;
    }

    public String getS3Key() {
        return s3Key;
    }

    public String getNomeArquivoOriginal() {
        return nomeArquivoOriginal;
    }

    public String getContentType() {
        return contentType;
    }

    public Long getTamanhoBytes() {
        return tamanhoBytes;
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
