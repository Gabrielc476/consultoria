package br.com.govflow.core.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "tb_convenios", schema = "core_schema")
public class ConvenioJpaEntity extends BaseTenantEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "prefeitura_id", nullable = false)
    private UUID prefeituraId;

    @Column(name = "numero_siconv", length = 30, nullable = false)
    private String numeroSiconv;

    @Column(name = "numero_processo", length = 50)
    private String numeroProcesso;

    @Column(name = "orgao_concedente", length = 150, nullable = false)
    private String orgaoConcedente;

    @Column(name = "objeto", nullable = false, columnDefinition = "TEXT")
    private String objeto;

    @Column(name = "valor_global", precision = 15, scale = 2, nullable = false)
    private BigDecimal valorGlobal;

    @Column(name = "valor_repasse", precision = 15, scale = 2, nullable = false)
    private BigDecimal valorRepasse;

    @Column(name = "valor_contrapartida", precision = 15, scale = 2, nullable = false)
    private BigDecimal valorContrapartida;

    @Column(name = "situacao", length = 50, nullable = false)
    private String situacao;

    @Column(name = "possui_clausula_suspensiva", nullable = false)
    private boolean possuiClausulaSuspensiva;

    @Column(name = "prazo_clausula_suspensiva")
    private LocalDate prazoClausulaSuspensiva;

    @Column(name = "data_inicio_vigencia")
    private LocalDate dataInicioVigencia;

    @Column(name = "data_fim_vigencia")
    private LocalDate dataFimVigencia;

    @Column(name = "prorrogacao_solicitada", nullable = false)
    private boolean prorrogacaoSolicitada;

    @Column(name = "novo_prazo_prorrogado")
    private LocalDate novoPrazoProrrogado;

    @Column(name = "s3_key_termo_retirada_suspensiva", length = 500)
    private String s3KeyTermoRetiradaSuspensiva;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ConvenioJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPrefeituraId() {
        return prefeituraId;
    }

    public void setPrefeituraId(UUID prefeituraId) {
        this.prefeituraId = prefeituraId;
    }

    public String getNumeroSiconv() {
        return numeroSiconv;
    }

    public void setNumeroSiconv(String numeroSiconv) {
        this.numeroSiconv = numeroSiconv;
    }

    public String getNumeroProcesso() {
        return numeroProcesso;
    }

    public void setNumeroProcesso(String numeroProcesso) {
        this.numeroProcesso = numeroProcesso;
    }

    public String getOrgaoConcedente() {
        return orgaoConcedente;
    }

    public void setOrgaoConcedente(String orgaoConcedente) {
        this.orgaoConcedente = orgaoConcedente;
    }

    public String getObjeto() {
        return objeto;
    }

    public void setObjeto(String objeto) {
        this.objeto = objeto;
    }

    public BigDecimal getValorGlobal() {
        return valorGlobal;
    }

    public void setValorGlobal(BigDecimal valorGlobal) {
        this.valorGlobal = valorGlobal;
    }

    public BigDecimal getValorRepasse() {
        return valorRepasse;
    }

    public void setValorRepasse(BigDecimal valorRepasse) {
        this.valorRepasse = valorRepasse;
    }

    public BigDecimal getValorContrapartida() {
        return valorContrapartida;
    }

    public void setValorContrapartida(BigDecimal valorContrapartida) {
        this.valorContrapartida = valorContrapartida;
    }

    public String getSituacao() {
        return situacao;
    }

    public void setSituacao(String situacao) {
        this.situacao = situacao;
    }

    public boolean isPossuiClausulaSuspensiva() {
        return possuiClausulaSuspensiva;
    }

    public void setPossuiClausulaSuspensiva(boolean possuiClausulaSuspensiva) {
        this.possuiClausulaSuspensiva = possuiClausulaSuspensiva;
    }

    public LocalDate getPrazoClausulaSuspensiva() {
        return prazoClausulaSuspensiva;
    }

    public void setPrazoClausulaSuspensiva(LocalDate prazoClausulaSuspensiva) {
        this.prazoClausulaSuspensiva = prazoClausulaSuspensiva;
    }

    public LocalDate getDataInicioVigencia() {
        return dataInicioVigencia;
    }

    public void setDataInicioVigencia(LocalDate dataInicioVigencia) {
        this.dataInicioVigencia = dataInicioVigencia;
    }

    public LocalDate getDataFimVigencia() {
        return dataFimVigencia;
    }

    public void setDataFimVigencia(LocalDate dataFimVigencia) {
        this.dataFimVigencia = dataFimVigencia;
    }

    public boolean isProrrogacaoSolicitada() {
        return prorrogacaoSolicitada;
    }

    public void setProrrogacaoSolicitada(boolean prorrogacaoSolicitada) {
        this.prorrogacaoSolicitada = prorrogacaoSolicitada;
    }

    public LocalDate getNovoPrazoProrrogado() {
        return novoPrazoProrrogado;
    }

    public void setNovoPrazoProrrogado(LocalDate novoPrazoProrrogado) {
        this.novoPrazoProrrogado = novoPrazoProrrogado;
    }

    public String getS3KeyTermoRetiradaSuspensiva() {
        return s3KeyTermoRetiradaSuspensiva;
    }

    public void setS3KeyTermoRetiradaSuspensiva(String s3KeyTermoRetiradaSuspensiva) {
        this.s3KeyTermoRetiradaSuspensiva = s3KeyTermoRetiradaSuspensiva;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
