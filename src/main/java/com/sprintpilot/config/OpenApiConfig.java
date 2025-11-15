package com.sprintpilot.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger Configuration for API Documentation
 * 
 * Access Swagger UI at: http://localhost:8080/swagger-ui.html
 * Access OpenAPI JSON at: http://localhost:8080/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${server.servlet.context-path:/}")
    private String contextPath;

    @Bean
    public OpenAPI sprintPilotOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SprintPilot API")
                        .description("Sprint Management System API - Manage your agile sprints, team members, tasks, and analytics")
                        .version("v1.0.0")
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server()
                                .url(String.format("http://localhost:%s%s", serverPort, contextPath))
                                .description("Local Development Server")
                ));
    }
}

