package com.frolovsnails.controller;

import com.frolovsnails.entity.*;
import com.frolovsnails.repository.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@Tag(name = "Test Controller", description = "Тестовые эндпоинты")
@RequiredArgsConstructor
public class TestController {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final ServiceRepository serviceRepository;
    private final WorkSlotRepository workSlotRepository;
    private final AppointmentRepository appointmentRepository;

    @GetMapping("/health")
    @Operation(summary = "Проверка здоровья сервиса")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "FrolovsNails",
                "timestamp", LocalDateTime.now().toString(),
                "database", "PostgreSQL",
                "tables", Map.of(
                        "users", userRepository.count(),
                        "clients", clientRepository.count(),
                        "services", serviceRepository.count(),
                        "work_slots", workSlotRepository.count(),
                        "appointments", appointmentRepository.count()
                )
        ));
    }

    @GetMapping("/db-status")
    @Operation(summary = "Статус подключения к БД")
    public ResponseEntity<Map<String, Object>> dbStatus() {
        try {
            long userCount = userRepository.count();
            return ResponseEntity.ok(Map.of(
                    "connected", true,
                    "message", "✅ Подключение к PostgreSQL успешно",
                    "userCount", userCount,
                    "hasData", userCount > 0
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "connected", false,
                    "message", "❌ Ошибка подключения к БД: " + e.getMessage(),
                    "userCount", 0,
                    "hasData", false
            ));
        }
    }

    @GetMapping("/public")
    @Operation(summary = "Публичный эндпоинт (доступен всем)")
    public ResponseEntity<Map<String, String>> publicEndpoint() {
        return ResponseEntity.ok(Map.of(
                "message", "✅ Это публичный эндпоинт - доступен без авторизации",
                "timestamp", LocalDateTime.now().toString(),
                "next", "Используйте /api/auth/register для регистрации"
        ));
    }

    @GetMapping("/secure")
    @Operation(summary = "Защищенный эндпоинт (требуется токен)")
    public ResponseEntity<Map<String, String>> secureEndpoint() {
        return ResponseEntity.ok(Map.of(
                "message", "🔒 Это защищенный эндпоинт - требуется JWT токен",
                "timestamp", LocalDateTime.now().toString(),
                "user", "Аутентифицированный пользователь"
        ));
    }
}