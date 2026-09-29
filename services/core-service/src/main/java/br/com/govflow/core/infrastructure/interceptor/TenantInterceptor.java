package br.com.govflow.core.infrastructure.interceptor;

import br.com.govflow.core.infrastructure.error.InvalidTenantHeaderException;
import br.com.govflow.core.infrastructure.error.MissingTenantHeaderException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class TenantInterceptor implements HandlerInterceptor {

    public static final String TENANT_HEADER = "X-Tenant-Id";
    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_ROLES_HEADER = "X-User-Roles";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String uri = request.getRequestURI();

        if (isPublicUri(uri, request.getMethod())) {
            // Rotas públicas que não exigem tenant prévio
            String tenantHeader = request.getHeader(TENANT_HEADER);
            if (tenantHeader != null && !tenantHeader.isBlank()) {
                try {
                    UUID tenantId = UUID.fromString(tenantHeader.trim());
                    TenantContext.setCurrentTenant(tenantId);
                    populateUserContextIfPresent(request, tenantId);
                } catch (IllegalArgumentException ignored) {
                    // Ignora em rotas públicas opcionais
                }
            }
            return true;
        }

        String tenantHeader = request.getHeader(TENANT_HEADER);
        if (tenantHeader == null || tenantHeader.isBlank()) {
            throw new MissingTenantHeaderException("Header X-Tenant-Id é obrigatório para acessar este recurso.");
        }

        try {
            UUID tenantId = UUID.fromString(tenantHeader.trim());
            TenantContext.setCurrentTenant(tenantId);
            populateUserContextIfPresent(request, tenantId);
            return true;
        } catch (IllegalArgumentException e) {
            throw new InvalidTenantHeaderException("Header X-Tenant-Id deve conter um UUID válido.");
        }
    }

    private void populateUserContextIfPresent(HttpServletRequest request, UUID tenantId) {
        String userIdHeader = request.getHeader(USER_ID_HEADER);
        if (userIdHeader != null && !userIdHeader.isBlank()) {
            try {
                UUID userId = UUID.fromString(userIdHeader.trim());
                String rolesHeader = request.getHeader(USER_ROLES_HEADER);
                Set<String> roles = Collections.emptySet();
                if (rolesHeader != null && !rolesHeader.isBlank()) {
                    roles = Arrays.stream(rolesHeader.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.toSet());
                }
                UserContext.setCurrentUser(userId, tenantId, roles, Collections.emptySet());
            } catch (IllegalArgumentException ignored) {
                // Se header de user_id for inválido, não quebra mas não autentica
            }
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TenantContext.clear();
        UserContext.clear();
    }

    private boolean isPublicUri(String uri, String method) {
        if (uri == null) return false;
        return uri.startsWith("/swagger-ui")
                || uri.startsWith("/v3/api-docs")
                || uri.startsWith("/actuator")
                || uri.startsWith("/error")
                || uri.startsWith("/api/v1/auth/login")
                || uri.startsWith("/api/v1/auth/register")
                || uri.startsWith("/api/v1/auth/register-consultoria")
                || (uri.startsWith("/api/v1/consultorias") && "POST".equalsIgnoreCase(method));
    }
}
