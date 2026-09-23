package com.proyecto.servicios.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApi {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Pago de Servicios y Catálogo de Productos")
                        .description("Especificación de servicios para gestión de personas y catálogo sincronizado de GestoPago")
                        .version("1.0.0")
                        .contact(new Contact().name("PuntoRed / GestoPago Integración")))
                .servers(List.of(
                        new Server().url("http://localhost:" + serverPort).description("Servidor Local")
                ));
    }
}
