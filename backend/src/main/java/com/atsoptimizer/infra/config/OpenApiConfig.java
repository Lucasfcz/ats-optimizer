package com.atsoptimizer.infra.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI atsOptimizerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ATS Optimizer API")
                        .description("API REST para upload, extração textual síncrona com Apache Tika e auditoria assíncrona de currículos orientada a padrões ATS com Spring AI e RabbitMQ.")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Lucas")
                                .email("lucasfc080@gmail.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")));
    }
}
