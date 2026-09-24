package br.com.govflow.transferegov.persistence.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_sincronizacao_log", schema = "transferegov_schema")
public class SincronizacaoLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tipo_sincronizacao", nullable = false, length = 50)
    private String tipoSincronizacao;

    @Column(name = "data_inicio", nullable = false)
    private OffsetDateTime dataInicio = OffsetDateTime.now();

    @Column(name = "data_fim")
    private OffsetDateTime dataFim;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "EM_ANDAMENTO";

    @Column(name = "data_carga_siconv_referencia", length = 50)
    private String dataCargaSiconvReferencia;

    @Column(name = "total_registros_lidos", nullable = false)
    private long totalRegistrosLidos = 0;

    @Column(name = "total_registros_filtrados", nullable = false)
    private long totalRegistrosFiltrados = 0;

    @Column(name = "total_registros_persistidos", nullable = false)
    private long totalRegistrosPersistidos = 0;

    @Column(name = "total_anomalias", nullable = false)
    private long totalAnomalias = 0;

    @Column(name = "tempo_execucao_ms")
    private Long tempoExecucaoMs;

    @Column(name = "detalhes_execucao_json", columnDefinition = "TEXT")
    private String detalhesExecucaoJson;

    @Column(name = "mensagem_erro", columnDefinition = "TEXT")
    private String mensagemErro;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public SincronizacaoLogEntity() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTipoSincronizacao() {
        return tipoSincronizacao;
    }

    public void setTipoSincronizacao(String tipoSincronizacao) {
        this.tipoSincronizacao = tipoSincronizacao;
    }

    public OffsetDateTime getDataInicio() {
        return dataInicio;
    }

    public void setDataInicio(OffsetDateTime dataInicio) {
        this.dataInicio = dataInicio;
    }

    public OffsetDateTime getDataFim() {
        return dataFim;
    }

    public void setDataFim(OffsetDateTime dataFim) {
        this.dataFim = dataFim;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDataCargaSiconvReferencia() {
        return dataCargaSiconvReferencia;
    }

    public void setDataCargaSiconvReferencia(String dataCargaSiconvReferencia) {
        this.dataCargaSiconvReferencia = dataCargaSiconvReferencia;
    }

    public long getTotalRegistrosLidos() {
        return totalRegistrosLidos;
    }

    public void setTotalRegistrosLidos(long totalRegistrosLidos) {
        this.totalRegistrosLidos = totalRegistrosLidos;
    }

    public long getTotalRegistrosFiltrados() {
        return totalRegistrosFiltrados;
    }

    public void setTotalRegistrosFiltrados(long totalRegistrosFiltrados) {
        this.totalRegistrosFiltrados = totalRegistrosFiltrados;
    }

    public long getTotalRegistrosPersistidos() {
        return totalRegistrosPersistidos;
    }

    public void setTotalRegistrosPersistidos(long totalRegistrosPersistidos) {
        this.totalRegistrosPersistidos = totalRegistrosPersistidos;
    }

    public long getTotalAnomalias() {
        return totalAnomalias;
    }

    public void setTotalAnomalias(long totalAnomalias) {
        this.totalAnomalias = totalAnomalias;
    }

    public Long getTempoExecucaoMs() {
        return tempoExecucaoMs;
    }

    public void setTempoExecucaoMs(Long tempoExecucaoMs) {
        this.tempoExecucaoMs = tempoExecucaoMs;
    }

    public String getDetalhesExecucaoJson() {
        return detalhesExecucaoJson;
    }

    public void setDetalhesExecucaoJson(String detalhesExecucaoJson) {
        this.detalhesExecucaoJson = detalhesExecucaoJson;
    }

    public String getMensagemErro() {
        return mensagemErro;
    }

    public void setMensagemErro(String mensagemErro) {
        this.mensagemErro = mensagemErro;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
