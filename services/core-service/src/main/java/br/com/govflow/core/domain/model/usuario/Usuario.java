package br.com.govflow.core.domain.model.usuario;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class Usuario {

    private final UUID id;
    private final UUID tenantId;
    private String nome;
    private final String email;
    private String senhaHash;
    private String telefoneCelular;
    private RoleUsuario role;
    private boolean ativo;
    private Set<UUID> prefeiturasAtribuidasIds;
    private final Instant createdAt;
    private Instant updatedAt;

    public Usuario(
            UUID id,
            UUID tenantId,
            String nome,
            String email,
            String senhaHash,
            String telefoneCelular,
            RoleUsuario role,
            boolean ativo,
            Set<UUID> prefeiturasAtribuidasIds,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "ID do usuário não pode ser nulo");
        this.tenantId = Objects.requireNonNull(tenantId, "Tenant ID não pode ser nulo");
        this.nome = Objects.requireNonNull(nome, "Nome do usuário não pode ser nulo").trim();
        this.email = Objects.requireNonNull(email, "E-mail não pode ser nulo").trim().toLowerCase();
        this.senhaHash = Objects.requireNonNull(senhaHash, "Senha hash não pode ser nula");
        this.telefoneCelular = telefoneCelular != null ? telefoneCelular.trim() : null;
        this.role = role != null ? role : RoleUsuario.AGENTE;
        this.ativo = ativo;
        this.prefeiturasAtribuidasIds = prefeiturasAtribuidasIds != null
                ? new HashSet<>(prefeiturasAtribuidasIds)
                : new HashSet<>();
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
    }

    public static Usuario criarNovoAdmin(UUID tenantId, String nome, String email, String senhaHash, String telefoneCelular) {
        return new Usuario(
                UUID.randomUUID(),
                tenantId,
                nome,
                email,
                senhaHash,
                telefoneCelular,
                RoleUsuario.ADMIN,
                true,
                Collections.emptySet(),
                Instant.now(),
                Instant.now()
        );
    }

    public static Usuario criarNovoAgente(UUID tenantId, String nome, String email, String senhaHash, String telefoneCelular, Set<UUID> prefeiturasIds) {
        return new Usuario(
                UUID.randomUUID(),
                tenantId,
                nome,
                email,
                senhaHash,
                telefoneCelular,
                RoleUsuario.AGENTE,
                true,
                prefeiturasIds,
                Instant.now(),
                Instant.now()
        );
    }

    public void atualizarDados(String nome, String telefoneCelular) {
        if (nome != null && !nome.isBlank()) {
            this.nome = nome.trim();
        }
        if (telefoneCelular != null) {
            this.telefoneCelular = telefoneCelular.trim();
        }
        this.updatedAt = Instant.now();
    }

    public void atualizarSenha(String novaSenhaHash) {
        this.senhaHash = Objects.requireNonNull(novaSenhaHash, "Nova senha hash não pode ser nula");
        this.updatedAt = Instant.now();
    }

    public void atribuirPrefeituras(Set<UUID> prefeiturasIds) {
        this.prefeiturasAtribuidasIds = prefeiturasIds != null ? new HashSet<>(prefeiturasIds) : new HashSet<>();
        this.updatedAt = Instant.now();
    }

    public void ativar() {
        this.ativo = true;
        this.updatedAt = Instant.now();
    }

    public void inativar() {
        this.ativo = false;
        this.updatedAt = Instant.now();
    }

    public boolean isAdmin() {
        return this.role == RoleUsuario.ADMIN;
    }

    public boolean isAgente() {
        return this.role == RoleUsuario.AGENTE;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public String getTelefoneCelular() {
        return telefoneCelular;
    }

    public RoleUsuario getRole() {
        return role;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public Set<UUID> getPrefeiturasAtribuidasIds() {
        return Collections.unmodifiableSet(prefeiturasAtribuidasIds);
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
        if (!(o instanceof Usuario usuario)) return false;
        return Objects.equals(id, usuario.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
