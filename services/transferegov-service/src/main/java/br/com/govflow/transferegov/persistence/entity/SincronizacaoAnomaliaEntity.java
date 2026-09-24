package br.com.govflow.transferegov.persistence.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_sincronizacao_anomalias", schema = "transferegov_schema")
public class SincronizacaoAnomaliaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "log_id")
    private UUID logId;

    @Column(name = "origem_arquivo", nullable = false, length = 100)
    private String origemArquivo;

    @Column(name = "numero_linha")
    private Long numeroLinha;

    @Column(name = "identificador_registro", length = 100)
    private String identificadorRegistro;

    @Column(name = "dimensao_qualidade", nullable = false, length = 50)
    private String dimensaoQualidade;

    @Column(name = "descricao_falha", nullable = false, columnDefinition = "TEXT")
    private String descricaoFalha;

    @Column(name = "conteudo_bruto", columnDefinition = "TEXT")
    private String conteudoBruto;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public SincronizacaoAnomaliaEntity() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getLogId() {
        return logId;
    }

    public void setLogId(UUID logId) {
        this.logId = logId;
    }

    public String getOrigemArquivo() {
        return origemArquivo;
    }

    public void setOrigemArquivo(String origemArquivo) {
        this.origemArquivo = origemArquivo;
    }

    public Long getNumeroLinha() {
        return numeroLinha;
    }

    public void setNumeroLinha(Long numeroLinha) {
        this.numeroLinha = numeroLinha;
    }

    public String getIdentificadorRegistro() {
        return identificadorRegistro;
    }

    public void setIdentificadorRegistro(String identificadorRegistro) {
        this.identificadorRegistro = identificadorRegistro;
    }

    public String getDimensaoQualidade() {
        return dimensaoQualidade;
    }

    public void setDimensaoQualidade(String dimensaoQualidade) {
        this.dimensaoQualidade = dimensaoQualidade;
    }

    public String getDescricaoFalha() {
        return descricaoFalha;
    }

    public void setDescricaoFalha(String descricaoFalha) {
        this.descricaoFalha = descricaoFalha;
    }

    public String getConteudoBruto() {
        return conteudoBruto;
    }

    public void setConteudoBruto(String conteudoBruto) {
        this.conteudoBruto = conteudoBruto;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
