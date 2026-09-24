package br.com.govflow.transferegov.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_sincronizacao_convenio", schema = "transferegov_schema")
public class SincronizacaoConvenioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "nr_convenio", nullable = false, unique = true, length = 30)
    private String nrConvenio;

    @Column(name = "id_proposta", length = 30)
    private String idProposta;

    @Column(name = "cnpj_proponente", nullable = false, length = 18)
    private String cnpjProponente;

    @Column(name = "nome_proponente", nullable = false, length = 200)
    private String nomeProponente;

    @Column(name = "municipio", nullable = false, length = 100)
    private String municipio;

    @Column(name = "uf", nullable = false, length = 2)
    private String uf;

    @Column(name = "situacao_convenio", length = 50)
    private String situacaoConvenio;

    @Column(name = "instrumento_ativo", nullable = false)
    private boolean instrumentoAtivo = true;

    @Column(name = "data_inicio_vigencia")
    private LocalDate dataInicioVigencia;

    @Column(name = "data_fim_vigencia")
    private LocalDate dataFimVigencia;

    @Column(name = "data_limite_prestacao_contas")
    private LocalDate dataLimitePrestacaoContas;

    @Column(name = "data_suspensiva")
    private LocalDate dataSuspensiva;

    @Column(name = "valor_global", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorGlobal = BigDecimal.ZERO;

    @Column(name = "valor_repasse", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorRepasse = BigDecimal.ZERO;

    @Column(name = "valor_contrapartida", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorContrapartida = BigDecimal.ZERO;

    @Column(name = "valor_saldo_conta", precision = 15, scale = 2)
    private BigDecimal valorSaldoConta = BigDecimal.ZERO;

    @Column(name = "objeto", columnDefinition = "TEXT")
    private String objeto;

    @Column(name = "data_carga_siconv", length = 50)
    private String dataCargaSiconv;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public SincronizacaoConvenioEntity() {}

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNrConvenio() {
        return nrConvenio;
    }

    public void setNrConvenio(String nrConvenio) {
        this.nrConvenio = nrConvenio;
    }

    public String getIdProposta() {
        return idProposta;
    }

    public void setIdProposta(String idProposta) {
        this.idProposta = idProposta;
    }

    public String getCnpjProponente() {
        return cnpjProponente;
    }

    public void setCnpjProponente(String cnpjProponente) {
        this.cnpjProponente = cnpjProponente;
    }

    public String getNomeProponente() {
        return nomeProponente;
    }

    public void setNomeProponente(String nomeProponente) {
        this.nomeProponente = nomeProponente;
    }

    public String getMunicipio() {
        return municipio;
    }

    public void setMunicipio(String municipio) {
        this.municipio = municipio;
    }

    public String getUf() {
        return uf;
    }

    public void setUf(String uf) {
        this.uf = uf;
    }

    public String getSituacaoConvenio() {
        return situacaoConvenio;
    }

    public void setSituacaoConvenio(String situacaoConvenio) {
        this.situacaoConvenio = situacaoConvenio;
    }

    public boolean isInstrumentoAtivo() {
        return instrumentoAtivo;
    }

    public void setInstrumentoAtivo(boolean instrumentoAtivo) {
        this.instrumentoAtivo = instrumentoAtivo;
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

    public LocalDate getDataLimitePrestacaoContas() {
        return dataLimitePrestacaoContas;
    }

    public void setDataLimitePrestacaoContas(LocalDate dataLimitePrestacaoContas) {
        this.dataLimitePrestacaoContas = dataLimitePrestacaoContas;
    }

    public LocalDate getDataSuspensiva() {
        return dataSuspensiva;
    }

    public void setDataSuspensiva(LocalDate dataSuspensiva) {
        this.dataSuspensiva = dataSuspensiva;
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

    public BigDecimal getValorSaldoConta() {
        return valorSaldoConta;
    }

    public void setValorSaldoConta(BigDecimal valorSaldoConta) {
        this.valorSaldoConta = valorSaldoConta;
    }

    public String getObjeto() {
        return objeto;
    }

    public void setObjeto(String objeto) {
        this.objeto = objeto;
    }

    public String getDataCargaSiconv() {
        return dataCargaSiconv;
    }

    public void setDataCargaSiconv(String dataCargaSiconv) {
        this.dataCargaSiconv = dataCargaSiconv;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
