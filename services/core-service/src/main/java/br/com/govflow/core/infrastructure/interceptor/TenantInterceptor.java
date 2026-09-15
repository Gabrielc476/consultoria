package br.com.govflow.core.infrastructure.interceptor;

import br.com.govflow.core.infrastructure.error.InvalidTenantHeaderException;
import br.com.govflow.core.infrastructure.error.MissingTenantHeaderException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Component
public class TenantInterceptor implements HandlerInterceptor {

    public static final String TENANT_HEADER = "X-Tenant-Id";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String uri = request.getRequestURI();

        if (isPublicUri(uri, request.getMethod())) {
            // Rotas públicas que não exigem tenant prévio
            String tenantHeader = request.getHeader(TENANT_HEADER);
            if (tenantHeader != null && !tenantHeader.isBlank()) {
                try {
                    TenantContext.setCurrentTenant(UUID.fromString(tenantHeader.trim()));
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
            return true;
        } catch (IllegalArgumentException e) {
            throw new InvalidTenantHeaderException("Header X-Tenant-Id deve conter um UUID válido.");
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TenantContext.clear();
    }

    private boolean isPublicUri(String uri, String method) {
        if (uri == null) return false;
        return uri.startsWith("/swagger-ui")
                || uri.startsWith("/v3/api-docs")
                || uri.startsWith("/actuator")
                || uri.startsWith("/error")
                || (uri.startsWith("/api/v1/consultorias") && "POST".equalsIgnoreCase(method));
    }
}
