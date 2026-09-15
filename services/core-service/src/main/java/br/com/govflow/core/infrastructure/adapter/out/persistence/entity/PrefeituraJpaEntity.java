package br.com.govflow.core.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "tb_prefeituras", schema = "core_schema")
public class PrefeituraJpaEntity extends BaseTenantEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "cnpj", length = 18, nullable = false)
    private String cnpj;

    @Column(name = "razao_social", length = 200, nullable = false)
    private String razaoSocial;

    @Column(name = "nome_municipio", length = 100, nullable = false)
    private String nomeMunicipio;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "uf", length = 2, nullable = false)
    private String uf;

    @Column(name = "codigo_ibge", length = 7, nullable = false)
    private String codigoIbge;

    @Column(name = "porte_municipio", length = 30, nullable = false)
    private String porteMunicipio;

    @Column(name = "nome_prefeito", length = 150)
    private String nomePrefeito;

    @Column(name = "cpf_prefeito", length = 14)
    private String cpfPrefeito;

    @Column(name = "inicio_mandato")
    private LocalDate inicioMandato;

    @Column(name = "fim_mandato")
    private LocalDate fimMandato;

    @Column(name = "status_cauc", length = 30, nullable = false)
    private String statusCauc;

    @Column(name = "ativo", nullable = false)
    private boolean ativo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public PrefeituraJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public String getRazaoSocial() {
        return razaoSocial;
    }

    public void setRazaoSocial(String razaoSocial) {
        this.razaoSocial = razaoSocial;
    }

    public String getNomeMunicipio() {
        return nomeMunicipio;
    }

    public void setNomeMunicipio(String nomeMunicipio) {
        this.nomeMunicipio = nomeMunicipio;
    }

    public String getUf() {
        return uf;
    }

    public void setUf(String uf) {
        this.uf = uf;
    }

    public String getCodigoIbge() {
        return codigoIbge;
    }

    public void setCodigoIbge(String codigoIbge) {
        this.codigoIbge = codigoIbge;
    }

    public String getPorteMunicipio() {
        return porteMunicipio;
    }

    public void setPorteMunicipio(String porteMunicipio) {
        this.porteMunicipio = porteMunicipio;
    }

    public String getNomePrefeito() {
        return nomePrefeito;
    }

    public void setNomePrefeito(String nomePrefeito) {
        this.nomePrefeito = nomePrefeito;
    }

    public String getCpfPrefeito() {
        return cpfPrefeito;
    }

    public void setCpfPrefeito(String cpfPrefeito) {
        this.cpfPrefeito = cpfPrefeito;
    }

    public LocalDate getInicioMandato() {
        return inicioMandato;
    }

    public void setInicioMandato(LocalDate inicioMandato) {
        this.inicioMandato = inicioMandato;
    }

    public LocalDate getFimMandato() {
        return fimMandato;
    }

    public void setFimMandato(LocalDate fimMandato) {
        this.fimMandato = fimMandato;
    }

    public String getStatusCauc() {
        return statusCauc;
    }

    public void setStatusCauc(String statusCauc) {
        this.statusCauc = statusCauc;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
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
