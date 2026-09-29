package br.com.govflow.core.infrastructure.interceptor;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class UserContext {

    public record UserData(
            UUID userId,
            UUID tenantId,
            Set<String> roles,
            Set<UUID> prefeiturasAtribuidasIds
    ) {
        public UserData {
            roles = roles != null ? Collections.unmodifiableSet(new HashSet<>(roles)) : Collections.emptySet();
            prefeiturasAtribuidasIds = prefeiturasAtribuidasIds != null
                    ? Collections.unmodifiableSet(new HashSet<>(prefeiturasAtribuidasIds))
                    : Collections.emptySet();
        }
    }

    private static final ThreadLocal<UserData> CURRENT_USER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void setCurrentUser(UserData data) {
        CURRENT_USER.set(data);
    }

    public static void setCurrentUser(UUID userId, UUID tenantId, Set<String> roles, Set<UUID> prefeiturasAtribuidasIds) {
        CURRENT_USER.set(new UserData(userId, tenantId, roles, prefeiturasAtribuidasIds));
    }

    public static UserData getCurrentUser() {
        return CURRENT_USER.get();
    }

    public static UUID getUserId() {
        UserData data = CURRENT_USER.get();
        return data != null ? data.userId() : null;
    }

    public static UUID getTenantId() {
        UserData data = CURRENT_USER.get();
        return data != null ? data.tenantId() : null;
    }

    public static Set<String> getRoles() {
        UserData data = CURRENT_USER.get();
        return data != null ? data.roles() : Collections.emptySet();
    }

    public static boolean isAdmin() {
        UserData data = CURRENT_USER.get();
        return data != null && data.roles().contains("ADMIN");
    }

    public static boolean isAgente() {
        UserData data = CURRENT_USER.get();
        return data != null && data.roles().contains("AGENTE");
    }

    public static Set<UUID> getPrefeiturasAtribuidasIds() {
        UserData data = CURRENT_USER.get();
        return data != null ? data.prefeiturasAtribuidasIds() : Collections.emptySet();
    }

    public static void setPrefeiturasAtribuidasIds(Set<UUID> prefeiturasIds) {
        UserData current = CURRENT_USER.get();
        if (current != null) {
            CURRENT_USER.set(new UserData(current.userId(), current.tenantId(), current.roles(), prefeiturasIds));
        }
    }

    public static boolean hasAccessToPrefeitura(UUID prefeituraId) {
        UserData data = CURRENT_USER.get();
        if (data == null) {
            return false;
        }
        if (isAdmin()) {
            return true;
        }
        return data.prefeiturasAtribuidasIds().contains(prefeituraId);
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
