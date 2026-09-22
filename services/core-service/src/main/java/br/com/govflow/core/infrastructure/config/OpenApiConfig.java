package br.com.govflow.core.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI coreOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("GovFlow API — Copiloto Transferegov")
                        .description("""
                                ### Documentação Interativa da API GovFlow
                                
                                Esta interface Swagger permite testar e inspecionar todos os contratos REST utilizados pelo frontend:
                                * **Autenticação:** Obtenha o token JWT em `/api/v1/auth/login`.
                                * **Documentos:** Listagem, detalhe, streaming de arquivo original e ciclo de conferência/aprovação.
                                * **Prefeituras & Consultorias:** Gestão de municípios convenentes e isolamento multi-tenant.
                                
                                **Como autenticar no Swagger:**
                                1. Execute o endpoint `POST /api/v1/auth/login` com as credenciais de teste (`email: gestor@demo.gov.br`, `senha: demo`).
                                2. Copie o `token` gerado.
                                3. Clique no botão verde **Authorize** no topo desta página, insira o token e confirme.
                                """)
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("GovFlow Engineering")
                                .email("engenharia@govflow.com.br")
                                .url("https://govflow.com.br"))
                        .license(new License()
                                .name("Proprietary — GovFlow 2026")
                                .url("https://govflow.com.br")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("API Gateway Reativo (Porta 8080 - Recomendado)"),
                        new Server().url("http://localhost:8081").description("Core Service Direto (Porta 8081 - Interno)")
                ))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Insira o token JWT de autenticação para autorizar as requisições.")));
    }
}
