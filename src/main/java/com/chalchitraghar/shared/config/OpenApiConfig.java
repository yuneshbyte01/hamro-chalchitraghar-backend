package com.chalchitraghar.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI hamroChalchitragharOpenAPI() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("Hamro Chalchitraghar Backend API")
                                .description(
                                        """
                                REST API for Hamro Chalchitraghar V2.
                                Includes public movie, hall, show and seat browsing; JWT authentication;
                                customer booking flow; staff booking lookup; and admin management endpoints.
                                All non-empty responses use the standard ApiResponse wrapper.
                                """)
                                .version("v2"))
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        BEARER_AUTH,
                                        new SecurityScheme()
                                                .name(BEARER_AUTH)
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")))
                .addTagsItem(
                        new Tag()
                                .name("Auth")
                                .description("Registration, login, and token refresh"))
                .addTagsItem(
                        new Tag()
                                .name("Public Movies")
                                .description("Public movie browsing endpoints"))
                .addTagsItem(
                        new Tag()
                                .name("Public Halls")
                                .description("Public hall browsing endpoints"))
                .addTagsItem(
                        new Tag()
                                .name("Public Shows")
                                .description("Public show and seat browsing endpoints"))
                .addTagsItem(
                        new Tag().name("Health").description("Public application health check"))
                .addTagsItem(
                        new Tag()
                                .name("Customer Bookings")
                                .description("Authenticated customer booking workflow"))
                .addTagsItem(new Tag().name("Staff").description("Staff booking lookup endpoints"))
                .addTagsItem(new Tag().name("Admin Movies").description("Admin movie management"))
                .addTagsItem(
                        new Tag()
                                .name("Admin Halls")
                                .description("Admin hall and seat layout management"))
                .addTagsItem(new Tag().name("Admin Shows").description("Admin show management"))
                .addTagsItem(
                        new Tag().name("Admin Users").description("Admin user lookup endpoints"));
    }
}
