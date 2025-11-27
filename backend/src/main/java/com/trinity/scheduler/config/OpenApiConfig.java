package com.trinity.scheduler.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for OpenAPI/Swagger documentation.
 * <p>
 * Provides API documentation for scheduling algorithms,
 * plus common endpoints for queries and enrollment.
 * <p>
 * Access Swagger UI at: /swagger-ui.html
 * Access API docs at: /v3/api-docs
 *
 * @author Atul Kumar
 */
@Configuration
public class OpenApiConfig {

    /**
     * Configures the OpenAPI specification for the Trinity College API.
     *
     * @return OpenAPI's configuration with metadata
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Trinity College Scheduler API")
                        .version("1.0.0")
                        .description("""
                                Automated scheduling system for Trinity College.
                                                                
                                Features:
                                - API: Course and Student scheduling
                                - API: Course and Student operations
                                                                
                                The system manages scheduling for 400 students, 50 teachers, and 60 classrooms
                                while respecting complex constraints including prerequisites, room types,
                                teacher workloads, and time conflicts.
                                """)
                        .contact(new Contact()
                                .name("Atul Kumar")
                                .email("kumar.atul.de@gmail.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }
}
