package com.jobportal.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenAPIConfig {

    private SecurityScheme createAPIKeyScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .bearerFormat("JWT")
                .scheme("bearer");
    }

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication", createAPIKeyScheme()))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Development Server"),
                        new Server()
                                .url("https://api.jobportal.com")
                                .description("Production Server")))
                .tags(List.of(
                        new Tag()
                                .name("Authentication")
                                .description("User authentication and registration endpoints"),
                        new Tag()
                                .name("Jobs")
                                .description("Job management and search operations"),
                        new Tag()
                                .name("Applications")
                                .description("Job application management"),
                        new Tag()
                                .name("Profiles")
                                .description("User profile management"),
                        new Tag()
                                .name("Admin")
                                .description("Administrative operations (Admin role required)"),
                        new Tag()
                                .name("Applicant")
                                .description("Applicant-specific operations"),
                        new Tag()
                                .name("Resume")
                                .description("Resume upload and management")))
                .info(new Info()
                        .title("Job Portal API")
                        .description("""
                                # Job Portal REST API
                                
                                A comprehensive REST API for a Job Portal application with role-based access control.
                                
                                ## Features
                                - **JWT Authentication**: Secure token-based authentication
                                - **Role-based Access Control**: ADMIN, COMPANY, and APPLICANT roles
                                - **Job Management**: Create, update, delete, and search job postings
                                - **Application Tracking**: Apply for jobs and track application status
                                - **Profile Management**: User profile creation and updates
                                - **Resume Upload**: File upload capabilities for resumes
                                - **Administrative Tools**: User and system management
                                
                                ## Authentication
                                Most endpoints require JWT authentication. Include the token in the Authorization header:
                                ```
                                Authorization: Bearer <your-jwt-token>
                                ```
                                
                                ## Response Format
                                All API responses follow a standardized format:
                                ```json
                                {
                                  "status": "success|error",
                                  "message": "Descriptive message",
                                  "data": { ... },
                                  "timestamp": "2024-11-16T10:30:00Z"
                                }
                                ```
                                
                                ## Error Handling
                                - **400 Bad Request**: Invalid input or missing required fields
                                - **401 Unauthorized**: Missing or invalid authentication token  
                                - **403 Forbidden**: Insufficient permissions for the requested operation
                                - **404 Not Found**: Requested resource not found
                                - **500 Internal Server Error**: Server-side error
                                
                                ## Rate Limiting
                                API requests are subject to rate limiting. Requests exceeding 20 seconds will timeout.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Job Portal Development Team")
                                .email("dev@jobportal.com")
                                .url("https://github.com/jobportal/api"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")));
    }
}