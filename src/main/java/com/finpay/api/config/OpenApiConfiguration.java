package com.finpay.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfiguration {

    @Bean
    OpenAPI finPayOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("FinPay API")
                        .version("1.0.0")
                        .description("Educational REST API that simulates the basic payment lifecycle."));
    }
}
