package com.frolovsnails.controller;

import com.frolovsnails.dto.request.LoginRequest;
import com.frolovsnails.dto.request.RegisterRequest;
import com.frolovsnails.dto.response.ApiResponse;
import com.frolovsnails.dto.response.AuthResponse;
import com.frolovsnails.entity.User;
import com.frolovsnails.repository.UserRepository;
import com.frolovsnails.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Аутентификация и регистрация")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    @Operation(summary = "Регистрация нового клиента")
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.ok(ApiResponse.success(
                    "Регистрация успешна",
                    response
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/login")
    @Operation(summary = "Вход в систему")
    public ResponseEntity<ApiResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(ApiResponse.success(
                    "Вход выполнен успешно",
                    response
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "Обновление токенов")
    public ResponseEntity<ApiResponse> refreshToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Отсутствует токен"));
            }

            String refreshToken = authHeader.substring(7);
            AuthResponse response = authService.refreshToken(refreshToken);

            return ResponseEntity.ok(ApiResponse.success(
                    "Токены обновлены",
                    response
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Выход из системы")
    public ResponseEntity<ApiResponse> logout() {
        // В stateless архитектуре logout происходит на клиенте (удаление токена)
        return ResponseEntity.ok(ApiResponse.success("Выход выполнен успешно"));
    }

    @PostMapping("/fcm-token")
    @Operation(summary = "Сохранить FCM токен для уведомлений")
    public ResponseEntity<ApiResponse> saveFcmToken(@RequestBody Map<String, String> request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String phone = auth.getName();

            User user = userRepository.findByPhone(phone)
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

            user.setFcmToken(request.get("token"));
            userRepository.save(user);

            return ResponseEntity.ok(ApiResponse.success("FCM токен сохранен"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}