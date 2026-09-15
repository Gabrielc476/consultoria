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
    private String nomePrefeito;
    private Cpf cpfPrefeito;
    private LocalDate inicioMandato;
    private LocalDate fimMandato;
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
                      String nomePrefeito,
                      Cpf cpfPrefeito,
                      LocalDate inicioMandato,
                      LocalDate fimMandato,
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
        this.nomePrefeito = nomePrefeito;
        this.cpfPrefeito = cpfPrefeito;
        this.inicioMandato = inicioMandato;
        this.fimMandato = fimMandato;
        this.statusCauc = statusCauc != null ? statusCauc : StatusCauc.ADIMPLENTE;
        this.ativo = ativo;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;

        validarInvariantes();
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
                nomePrefeito,
                cpfPrefeito,
                inicioMandato,
                fimMandato,
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

        if (inicioMandato != null && fimMandato != null && fimMandato.isBefore(inicioMandato)) {
            throw new IllegalArgumentException("A data final do mandato não pode ser anterior à data inicial.");
        }
    }

    public void atualizarDadosCadastrais(String razaoSocial,
                                        String nomeMunicipio,
                                        PorteMunicipio porteMunicipio,
                                        String nomePrefeito,
                                        Cpf cpfPrefeito,
                                        LocalDate inicioMandato,
                                        LocalDate fimMandato,
                                        StatusCauc statusCauc) {
        this.razaoSocial = requireNonBlank(razaoSocial, "Razão Social não pode ser vazia.");
        this.nomeMunicipio = requireNonBlank(nomeMunicipio, "Nome do Município não pode ser vazio.");
        if (porteMunicipio != null) {
            this.porteMunicipio = porteMunicipio;
        }
        this.nomePrefeito = nomePrefeito;
        this.cpfPrefeito = cpfPrefeito;
        this.inicioMandato = inicioMandato;
        this.fimMandato = fimMandato;
        if (statusCauc != null) {
            this.statusCauc = statusCauc;
        }
        this.updatedAt = Instant.now();
        validarInvariantes();
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

    public String getNomePrefeito() {
        return nomePrefeito;
    }

    public Cpf getCpfPrefeito() {
        return cpfPrefeito;
    }

    public LocalDate getInicioMandato() {
        return inicioMandato;
    }

    public LocalDate getFimMandato() {
        return fimMandato;
    }

    public StatusCauc getStatusCauc() {
        return statusCauc;
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
