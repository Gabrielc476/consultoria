package br.com.govflow.core.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI coreOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("GovFlow Core Service API")
                        .description("API Transacional do Core GovFlow - Gestão de Consultorias, Prefeituras, Convênios e Multi-Tenancy")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("GovFlow Engineering")
                                .email("suporte@govflow.com.br"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://govflow.com.br")));
    }
}
