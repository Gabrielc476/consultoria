package br.com.govflow.gateway.infrastructure.filter;

public final class GatewayHeaders {

    public static final String TENANT_ID = "X-Tenant-Id";
    public static final String USER_ID = "X-User-Id";
    public static final String USER_ROLES = "X-User-Roles";
    public static final String CORRELATION_ID = "X-Correlation-Id";

    private GatewayHeaders() {
    }
}
