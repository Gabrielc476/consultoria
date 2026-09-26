package br.com.govflow.gateway.infrastructure.config;

import br.com.govflow.gateway.infrastructure.config.GovflowProperties.Routes;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutesConfig {

    @Bean
    public RouteLocator govflowRoutes(RouteLocatorBuilder builder, GovflowProperties properties) {
        Routes routes = properties.getRoutes();

        return builder.routes()
                .route("auth-public", r -> r
                        .path("/api/v1/auth/**")
                        .uri(routes.getCoreUri()))
                .route("core-service", r -> r
                        .path("/api/v1/core/**")
                        .uri(routes.getCoreUri()))
                .route("core-documentos", r -> r
                        .path("/api/v1/documentos/**")
                        .uri(routes.getCoreUri()))
                .route("core-prefeituras", r -> r
                        .path("/api/v1/prefeituras/**")
                        .uri(routes.getCoreUri()))
                .route("core-consultorias", r -> r
                        .path("/api/v1/consultorias/**")
                        .uri(routes.getCoreUri()))
                .route("core-cauc", r -> r
                        .path("/api/v1/cauc/**")
                        .uri(routes.getCoreUri()))
                .route("core-convenios", r -> r
                        .path("/api/v1/convenios/**")
                        .uri(routes.getCoreUri()))
                .route("whatsapp-service", r -> r
                        .path("/api/v1/whatsapp/**")
                        .uri(routes.getWhatsappUri()))
                .route("ai-service", r -> r
                        .path("/api/v1/ai/**")
                        .uri(routes.getAiUri()))
                .route("transferegov-service", r -> r
                        .path("/api/v1/transferegov/**")
                        .uri(routes.getTransferegovUri()))
                .route("swagger-ui", r -> r
                        .path("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
                        .uri(routes.getCoreUri()))
                .build();
    }
}
