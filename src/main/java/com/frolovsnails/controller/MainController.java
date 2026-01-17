package com.frolovsnails.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Main Controller", description = "Основные эндпоинты")
public class MainController {

    @GetMapping("/health")
    @Operation(summary = "Проверка здоровья сервиса")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "FrolovsNails",
                "timestamp", LocalDateTime.now().toString(),
                "message", "✅ Сервис работает!"
        ));
    }

    @GetMapping("/hello")
    @Operation(summary = "Приветствие")
    public ResponseEntity<String> hello(@RequestParam(required = false) String name) {
        String greeting = name != null ?
                "Привет, " + name + "!" :
                "Привет от Frolov's Nails!";
        return ResponseEntity.ok(greeting);
    }

    @GetMapping("/test")
    @Operation(summary = "Тестовый эндпоинт")
    public ResponseEntity<Map<String, String>> test() {
        return ResponseEntity.ok(Map.of(
                "endpoint", "/api/test",
                "description", "Тестовый эндпоинт работает",
                "nextStep", "Настроить PostgreSQL"
        ));
    }
}