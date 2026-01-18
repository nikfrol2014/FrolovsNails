package com.frolovsnails.controller;

import com.frolovsnails.entity.*;
import com.frolovsnails.repository.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @Operation(summary = "Проверка здоровья сервиса (публичный)")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "FrolovsNails",
                "timestamp", LocalDateTime.now().toString(),
                "message", "✅ Сервис работает"
        ));
    }

    @GetMapping("/db-status")
    @Operation(summary = "Статус подключения к БД (публичный)")
    public ResponseEntity<Map<String, Object>> dbStatus() {
        try {
            long userCount = userRepository.count();
            return ResponseEntity.ok(Map.of(
                    "connected", true,
                    "message", "✅ Подключение к PostgreSQL успешно",
                    "userCount", userCount
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "connected", false,
                    "message", "❌ Ошибка подключения к БД: " + e.getMessage()
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
    @Operation(summary = "Защищенный эндпоинт (требуется любой токен)")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> secureEndpoint() {
        return ResponseEntity.ok(Map.of(
                "message", "🔒 Это защищенный эндпоинт - вы успешно аутентифицированы!",
                "timestamp", LocalDateTime.now().toString(),
                "access", "Требуется любой валидный JWT токен"
        ));
    }

    @GetMapping("/client-only")
    @Operation(summary = "Только для клиентов")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<Map<String, String>> clientOnly() {
        return ResponseEntity.ok(Map.of(
                "message", "👤 Этот эндпоинт доступен только клиентам",
                "timestamp", LocalDateTime.now().toString(),
                "role", "CLIENT"
        ));
    }

    @GetMapping("/admin-only")
    @Operation(summary = "Только для администраторов (мастеров)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> adminOnly() {
        return ResponseEntity.ok(Map.of(
                "message", "⚡ Этот эндпоинт доступен только мастерам (админам)",
                "timestamp", LocalDateTime.now().toString(),
                "role", "ADMIN"
        ));
    }

    @GetMapping("/stats")
    @Operation(summary = "Статистика БД (только для админов)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(Map.of(
                "users", userRepository.count(),
                "clients", clientRepository.count(),
                "services", serviceRepository.count(),
                "work_slots", workSlotRepository.count(),
                "appointments", appointmentRepository.count()
        ));
    }
}