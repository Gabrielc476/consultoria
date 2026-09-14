package br.com.govflow.gateway.domain.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RoutePolicyTest {

    @Test
    void publicAuthPathsAreOpen() {
        assertTrue(RoutePolicy.isPublicPath("/api/v1/auth/login"));
        assertTrue(RoutePolicy.isPublicPath("/api/v1/auth/refresh"));
        assertTrue(RoutePolicy.isPublicPath("/api/v1/auth"));
    }

    @Test
    void publicWhatsappWebhookPathsAreOpen() {
        assertTrue(RoutePolicy.isPublicPath("/api/v1/whatsapp/webhook/evolution"));
        assertTrue(RoutePolicy.isPublicPath("/api/v1/whatsapp/webhook"));
    }

    @Test
    void protectedPathsRequireAuth() {
        assertFalse(RoutePolicy.isPublicPath("/api/v1/core/prefeituras"));
        assertFalse(RoutePolicy.isPublicPath("/api/v1/whatsapp/messages"));
        assertFalse(RoutePolicy.isPublicPath("/api/v1/ai/extract"));
        assertFalse(RoutePolicy.isPublicPath("/api/v1/transferegov/convenios"));
    }
}
