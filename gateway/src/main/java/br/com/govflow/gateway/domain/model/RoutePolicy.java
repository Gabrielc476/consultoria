package br.com.govflow.gateway.domain.model;

import java.util.List;

/**
 * Defines which request paths are public (no JWT) vs protected.
 */
public final class RoutePolicy {

    private static final List<String> PUBLIC_PREFIXES = List.of(
            "/api/v1/auth/",
            "/api/v1/whatsapp/webhook/",
            "/swagger-ui/",
            "/v3/api-docs/"
    );

    private RoutePolicy() {
    }

    public static boolean isPublicPath(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        String normalized = path.endsWith("/") ? path : path + "/";
        // Exact match without trailing slash variants
        if ("/api/v1/auth".equals(path)
                || "/api/v1/whatsapp/webhook".equals(path)
                || "/swagger-ui.html".equals(path)
                || "/v3/api-docs".equals(path)) {
            return true;
        }
        for (String prefix : PUBLIC_PREFIXES) {
            if (path.startsWith(prefix) || normalized.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
