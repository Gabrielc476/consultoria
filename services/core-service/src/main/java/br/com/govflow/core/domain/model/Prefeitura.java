package br.com.govflow.core.domain.model;

import br.com.govflow.core.domain.exception.DomainException;
import br.com.govflow.core.domain.exception.TenantInvalidoException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public class Prefeitura {

    private final UUID id;
    private final UUID tenantId;
    private final Cnpj cnpj;
    private String razaoSocial;
    private String nomeMunicipio;
    private final Uf uf;
    private final CodigoIbge codigoIbge;
    private PorteMunicipio porteMunicipio;
    private MandatoGestor mandato;
    private StatusCauc statusCauc;
    private boolean ativo;
    private final Instant createdAt;
    private Instant updatedAt;

    public Prefeitura(UUID id,
                      UUID tenantId,
                      Cnpj cnpj,
                      String razaoSocial,
                      String nomeMunicipio,
                      Uf uf,
                      CodigoIbge codigoIbge,
                      PorteMunicipio porteMunicipio,
                      MandatoGestor mandato,
                      StatusCauc statusCauc,
                      boolean ativo,
                      Instant createdAt,
                      Instant updatedAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = Objects.requireNonNull(tenantId, "TenantId é obrigatório para Prefeitura.");
        this.cnpj = Objects.requireNonNull(cnpj, "CNPJ é obrigatório para Prefeitura.");
        this.razaoSocial = requireNonBlank(razaoSocial, "Razão Social não pode ser vazia.");
        this.nomeMunicipio = requireNonBlank(nomeMunicipio, "Nome do Município não pode ser vazio.");
        this.uf = Objects.requireNonNull(uf, "UF é obrigatória para Prefeitura.");
        this.codigoIbge = Objects.requireNonNull(codigoIbge, "Código IBGE é obrigatório para Prefeitura.");
        this.porteMunicipio = porteMunicipio != null ? porteMunicipio : PorteMunicipio.PEQUENO_PORTE_1;
        this.mandato = mandato;
        this.statusCauc = statusCauc != null ? statusCauc : StatusCauc.ADIMPLENTE;
        this.ativo = ativo;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;

        validarInvariantes();
    }

    public Prefeitura(UUID id,
                      UUID tenantId,
                      Cnpj cnpj,
                      String razaoSocial,
                      String nomeMunicipio,
                      Uf uf,
                      CodigoIbge codigoIbge,
                      PorteMunicipio porteMunicipio,
                      String nomePrefeito,
                      Cpf cpfPrefeito,
                      LocalDate inicioMandato,
                      LocalDate fimMandato,
                      StatusCauc statusCauc,
                      boolean ativo,
                      Instant createdAt,
                      Instant updatedAt) {
        this(
                id,
                tenantId,
                cnpj,
                razaoSocial,
                nomeMunicipio,
                uf,
                codigoIbge,
                porteMunicipio,
                (nomePrefeito != null || cpfPrefeito != null || inicioMandato != null || fimMandato != null)
                        ? new MandatoGestor(nomePrefeito, cpfPrefeito, inicioMandato, fimMandato)
                        : null,
                statusCauc,
                ativo,
                createdAt,
                updatedAt
        );
    }

    public static Prefeitura criarNova(UUID tenantId,
                                      Cnpj cnpj,
                                      String razaoSocial,
                                      String nomeMunicipio,
                                      Uf uf,
                                      CodigoIbge codigoIbge,
                                      PorteMunicipio porteMunicipio,
                                      String nomePrefeito,
                                      Cpf cpfPrefeito,
                                      LocalDate inicioMandato,
                                      LocalDate fimMandato) {
        return new Prefeitura(
                UUID.randomUUID(),
                tenantId,
                cnpj,
                razaoSocial,
                nomeMunicipio,
                uf,
                codigoIbge,
                porteMunicipio,
                (nomePrefeito != null || cpfPrefeito != null || inicioMandato != null || fimMandato != null)
                        ? new MandatoGestor(nomePrefeito, cpfPrefeito, inicioMandato, fimMandato)
                        : null,
                StatusCauc.ADIMPLENTE,
                true,
                Instant.now(),
                Instant.now()
        );
    }

    private void validarInvariantes() {
        if (tenantId == null) {
            throw new TenantInvalidoException("Tenant ID da prefeitura não pode ser nulo.");
        }
        codigoIbge.validarCompatibilidadeUf(uf);
    }

    public void atualizarDadosCadastrais(String razaoSocial,
                                        String nomeMunicipio,
                                        PorteMunicipio porteMunicipio,
                                        MandatoGestor mandato,
                                        StatusCauc statusCauc) {
        this.razaoSocial = requireNonBlank(razaoSocial, "Razão Social não pode ser vazia.");
        this.nomeMunicipio = requireNonBlank(nomeMunicipio, "Nome do Município não pode ser vazio.");
        if (porteMunicipio != null) {
            this.porteMunicipio = porteMunicipio;
        }
        this.mandato = mandato;
        if (statusCauc != null) {
            this.statusCauc = statusCauc;
        }
        this.updatedAt = Instant.now();
        validarInvariantes();
    }

    public void atualizarDadosCadastrais(String razaoSocial,
                                        String nomeMunicipio,
                                        PorteMunicipio porteMunicipio,
                                        String nomePrefeito,
                                        Cpf cpfPrefeito,
                                        LocalDate inicioMandato,
                                        LocalDate fimMandato,
                                        StatusCauc statusCauc) {
        atualizarDadosCadastrais(
                razaoSocial,
                nomeMunicipio,
                porteMunicipio,
                (nomePrefeito != null || cpfPrefeito != null || inicioMandato != null || fimMandato != null)
                        ? new MandatoGestor(nomePrefeito, cpfPrefeito, inicioMandato, fimMandato)
                        : null,
                statusCauc
        );
    }

    public void inativar() {
        this.ativo = false;
        this.updatedAt = Instant.now();
    }

    public void ativar() {
        this.ativo = true;
        this.updatedAt = Instant.now();
    }

    public void atualizarStatusCauc(StatusCauc novoStatus) {
        this.statusCauc = Objects.requireNonNull(novoStatus, "Status CAUC não pode ser nulo.");
        this.updatedAt = Instant.now();
    }

    public void atualizarSituacaoFiscal(SituacaoRegularidadeFiscal novaSituacao) {
        Objects.requireNonNull(novaSituacao, "Situação de regularidade fiscal não pode ser nula.");
        this.statusCauc = novaSituacao.toStatusCauc();
        this.updatedAt = Instant.now();
    }

    private static String requireNonBlank(String str, String message) {
        if (str == null || str.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return str.trim();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public Cnpj getCnpj() {
        return cnpj;
    }

    public String getRazaoSocial() {
        return razaoSocial;
    }

    public String getNomeMunicipio() {
        return nomeMunicipio;
    }

    public Uf getUf() {
        return uf;
    }

    public CodigoIbge getCodigoIbge() {
        return codigoIbge;
    }

    public PorteMunicipio getPorteMunicipio() {
        return porteMunicipio;
    }

    public MandatoGestor getMandato() {
        return mandato;
    }

    public String getNomePrefeito() {
        return mandato != null ? mandato.nomePrefeito() : null;
    }

    public Cpf getCpfPrefeito() {
        return mandato != null ? mandato.cpfPrefeito() : null;
    }

    public LocalDate getInicioMandato() {
        return mandato != null ? mandato.inicioMandato() : null;
    }

    public LocalDate getFimMandato() {
        return mandato != null ? mandato.fimMandato() : null;
    }

    public StatusCauc getStatusCauc() {
        return statusCauc;
    }

    public SituacaoRegularidadeFiscal getSituacaoRegularidadeFiscal() {
        return SituacaoRegularidadeFiscal.fromStatusCauc(statusCauc);
    }

    public boolean isAtivo() {
        return ativo;
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
        Prefeitura that = (Prefeitura) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
