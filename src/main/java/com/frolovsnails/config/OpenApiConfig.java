package com.frolovsnails.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Frolov's Nails API",
                version = "1.0.0",
                description = "API для студии маникюра",
                contact = @Contact(
                        name = "Поддержка",
                        email = "support@frolovsnails.ru"
                ),
                license = @License(
                        name = "Apache 2.0",
                        url = "http://springdoc.org"
                )
        ),
        servers = {
                @Server(
                        url = "http://localhost:8080",
                        description = "Локальный сервер разработки"
                )
        },
        security = @SecurityRequirement(name = "bearerAuth")  // Применяет безопасность ко всем эндпоинтам
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Введите JWT токен в формате: Bearer <ваш_токен>"
)
public class OpenApiConfig {
    // Конфигурация готова через аннотации
}