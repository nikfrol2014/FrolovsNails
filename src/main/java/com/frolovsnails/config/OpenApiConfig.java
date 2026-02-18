package com.frolovsnails.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Frolov's Nails API",
                version = "1.0.0",
                description = "API для студии маникюра\n\n" +
                        "**Форматы даты и времени:**\n" +
                        "* 📅 Дата: `dd.MM.yyyy` (например, 18.02.2026)\n" +
                        "* ⏰ Время: `HH:mm` (например, 14:30)\n" +
                        "* 📆 Дата и время: `dd.MM.yyyy HH:mm` (например, 18.02.2026 14:30)\n" +
                        "* 📋 В JSON ответах: `yyyy-MM-dd HH:mm:ss` (например, 2026-02-18 14:30:00)\n\n" +
                        "**Часовой пояс:** Europe/Moscow (UTC+3)",
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
                ),
                @Server(
                        url = "http://192.168.0.151:8080", // ip на работе
                        description = "Локальная сеть"
                )
        },
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Введите JWT токен в формате: Bearer <ваш_токен>"
)
public class OpenApiConfig {

    /**
     * Кастомизация Swagger UI для правильного отображения форматов дат
     */
    @Bean
    public OpenApiCustomizer openApiDateTimeCustomizer() {
        return openApi -> {
            var schemas = openApi.getComponents().getSchemas();
            if (schemas != null) {

                // Для LocalDate
                Schema<?> localDateSchema = schemas.get("LocalDate");
                if (localDateSchema == null) {
                    // Если схемы нет, создаем новую
                    localDateSchema = new StringSchema();
                    schemas.put("LocalDate", localDateSchema);
                }
                localDateSchema.setExample("18.02.2026");
                localDateSchema.setPattern("dd\\.MM\\.yyyy");
                localDateSchema.setDescription("Дата в формате dd.MM.yyyy (например, 18.02.2026)");

                // Для LocalTime
                Schema<?> localTimeSchema = schemas.get("LocalTime");
                if (localTimeSchema == null) {
                    localTimeSchema = new StringSchema();
                    schemas.put("LocalTime", localTimeSchema);
                }
                localTimeSchema.setExample("14:30");
                localTimeSchema.setPattern("HH:mm");
                localTimeSchema.setDescription("Время в формате HH:mm (например, 14:30)");

                // Для LocalDateTime
                Schema<?> localDateTimeSchema = schemas.get("LocalDateTime");
                if (localDateTimeSchema == null) {
                    localDateTimeSchema = new StringSchema();
                    schemas.put("LocalDateTime", localDateTimeSchema);
                }
                localDateTimeSchema.setExample("18.02.2026 14:30");
                localDateTimeSchema.setPattern("dd\\.MM\\.yyyy HH:mm");
                localDateTimeSchema.setDescription("Дата и время в формате dd.MM.yyyy HH:mm (например, 18.02.2026 14:30)");
            }
        };
    }

    /**
     * Альтернативный подход через Map схем (если нужно больше контроля)
     */
    @Bean
    public OpenApiCustomizer openApiCustomizer() {
        return openApi -> {
            openApi.getComponents().addSchemas("LocalDate",
                    new StringSchema()
                            .example("18.02.2026")
                            .pattern("dd\\.MM\\.yyyy")
                            .description("Дата в формате dd.MM.yyyy")
            );

            openApi.getComponents().addSchemas("LocalTime",
                    new StringSchema()
                            .example("14:30")
                            .pattern("HH:mm")
                            .description("Время в формате HH:mm")
            );

            openApi.getComponents().addSchemas("LocalDateTime",
                    new StringSchema()
                            .example("18.02.2026 14:30")
                            .pattern("dd\\.MM\\.yyyy HH:mm")
                            .description("Дата и время в формате dd.MM.yyyy HH:mm")
            );
        };
    }
}