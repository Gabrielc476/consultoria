package br.com.govflow.core.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Consultoria {

    private final UUID id;
    private final Cnpj cnpj;
    private String razaoSocial;
    private String nomeFantasia;
    private String emailContato;
    private String telefoneContato;
    private PlanoConsultoria plano;
    private StatusConsultoria status;
    private int limitePrefeituras;
    private final Instant createdAt;
    private Instant updatedAt;

    public Consultoria(UUID id,
                       Cnpj cnpj,
                       String razaoSocial,
                       String nomeFantasia,
                       String emailContato,
                       String telefoneContato,
                       PlanoConsultoria plano,
                       StatusConsultoria status,
                       int limitePrefeituras,
                       Instant createdAt,
                       Instant updatedAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.cnpj = Objects.requireNonNull(cnpj, "CNPJ é obrigatório para Consultoria.");
        this.razaoSocial = requireNonBlank(razaoSocial, "Razão Social não pode ser vazia.");
        this.nomeFantasia = requireNonBlank(nomeFantasia, "Nome Fantasia não pode ser vazio.");
        this.emailContato = requireNonBlank(emailContato, "E-mail de contato não pode ser vazio.");
        this.telefoneContato = telefoneContato;
        this.plano = plano != null ? plano : PlanoConsultoria.STARTER;
        this.status = status != null ? status : StatusConsultoria.ATIVO;
        this.limitePrefeituras = limitePrefeituras > 0 ? limitePrefeituras : this.plano.getLimitePrefeituras();
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
    }

    public static Consultoria criarNova(Cnpj cnpj,
                                        String razaoSocial,
                                        String nomeFantasia,
                                        String emailContato,
                                        String telefoneContato,
                                        PlanoConsultoria plano) {
        PlanoConsultoria planoEscolhido = plano != null ? plano : PlanoConsultoria.STARTER;
        return new Consultoria(
                UUID.randomUUID(),
                cnpj,
                razaoSocial,
                nomeFantasia,
                emailContato,
                telefoneContato,
                planoEscolhido,
                StatusConsultoria.ATIVO,
                planoEscolhido.getLimitePrefeituras(),
                Instant.now(),
                Instant.now()
        );
    }

    public void alterarPlano(PlanoConsultoria novoPlano) {
        this.plano = Objects.requireNonNull(novoPlano, "Novo plano não pode ser nulo.");
        this.limitePrefeituras = novoPlano.getLimitePrefeituras();
        this.updatedAt = Instant.now();
    }

    public void suspender() {
        this.status = StatusConsultoria.SUSPENSO;
        this.updatedAt = Instant.now();
    }

    public void reativar() {
        this.status = StatusConsultoria.ATIVO;
        this.updatedAt = Instant.now();
    }

    public void cancelar() {
        this.status = StatusConsultoria.CANCELADO;
        this.updatedAt = Instant.now();
    }

    public boolean podeCadastrarPrefeitura(int contagemAtualPrefeituras) {
        return this.status == StatusConsultoria.ATIVO && contagemAtualPrefeituras < this.limitePrefeituras;
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

    public Cnpj getCnpj() {
        return cnpj;
    }

    public String getRazaoSocial() {
        return razaoSocial;
    }

    public String getNomeFantasia() {
        return nomeFantasia;
    }

    public String getEmailContato() {
        return emailContato;
    }

    public String getTelefoneContato() {
        return telefoneContato;
    }

    public PlanoConsultoria getPlano() {
        return plano;
    }

    public StatusConsultoria getStatus() {
        return status;
    }

    public int getLimitePrefeituras() {
        return limitePrefeituras;
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
        Consultoria that = (Consultoria) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
