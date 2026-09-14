package br.com.govflow.gateway.domain.model;

import java.util.Set;
import java.util.UUID;

public record UserAuthentication(UUID userId, String username, Set<String> roles, TenantContext tenant) {

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }
}
