package com.frolovsnails.controller;

import com.frolovsnails.dto.request.UpdatePasswordRequest;
import com.frolovsnails.dto.request.UpdateProfileRequest;
import com.frolovsnails.dto.response.ApiResponse;
import com.frolovsnails.entity.Client;
import com.frolovsnails.entity.User;
import com.frolovsnails.repository.ClientRepository;
import com.frolovsnails.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@Tag(name = "Profile", description = "Управление профилем пользователя")
@RequiredArgsConstructor
public class ProfileController {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    @Operation(summary = "Получить свой профиль")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> getMyProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String phone = auth.getName();

        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // Для клиентов добавляем профиль клиента
        if (user.getRole() == com.frolovsnails.entity.Role.CLIENT) {
            Client client = clientRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new RuntimeException("Профиль клиента не найден"));

            return ResponseEntity.ok(ApiResponse.success(
                    "Профиль клиента",
                    Map.of(
                            "user", Map.of(
                                    "id", user.getId(),
                                    "phone", user.getPhone(),
                                    "role", user.getRole(),
                                    "createdAt", user.getCreatedAt()
                            ),
                            "client", Map.of(
                                    "id", client.getId(),
                                    "firstName", client.getFirstName(),
                                    "lastName", client.getLastName(),
                                    "birthDate", client.getBirthDate(),
                                    "notes", client.getNotes(),
                                    "createdAt", client.getCreatedAt()
                            )
                    )
            ));
        } else {
            // Для мастера (ADMIN)
            return ResponseEntity.ok(ApiResponse.success(
                    "Профиль мастера",
                    Map.of(
                            "user", Map.of(
                                    "id", user.getId(),
                                    "phone", user.getPhone(),
                                    "role", user.getRole(),
                                    "createdAt", user.getCreatedAt()
                            ),
                            "isMaster", true,
                            "note", "Мастер имеет доступ ко всем функциям системы"
                    )
            ));
        }
    }

    @PutMapping
    @Operation(summary = "Обновить профиль (для клиентов)")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ApiResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String phone = auth.getName();

        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        Client client = clientRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Профиль клиента не найден"));

        // Обновляем данные клиента
        if (request.getFirstName() != null) {
            client.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            client.setLastName(request.getLastName());
        }
        if (request.getBirthDate() != null) {
            client.setBirthDate(request.getBirthDate());
        }
        if (request.getNotes() != null) {
            client.setNotes(request.getNotes());
        }

        clientRepository.save(client);

        return ResponseEntity.ok(ApiResponse.success(
                "✅ Профиль обновлен успешно",
                Map.of(
                        "client", Map.of(
                                "firstName", client.getFirstName(),
                                "lastName", client.getLastName(),
                                "birthDate", client.getBirthDate()
                        ),
                        "updatedAt", LocalDate.now()
                )
        ));
    }

    @PatchMapping("/password")
    @Operation(summary = "Изменить пароль")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> changePassword(@Valid @RequestBody UpdatePasswordRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String phone = auth.getName();

        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // Проверяем старый пароль
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("❌ Неверный старый пароль")
            );
        }

        // Проверяем, что новый пароль не совпадает со старым
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("❌ Новый пароль не должен совпадать со старым")
            );
        }

        // Обновляем пароль
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return ResponseEntity.ok(ApiResponse.success("✅ Пароль успешно изменен"));
    }

    @GetMapping("/stats")
    @Operation(summary = "Получить статистику профиля (для клиентов)")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ApiResponse> getProfileStats() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String phone = auth.getName();

        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        Client client = clientRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Профиль клиента не найден"));

        // Здесь можно добавить логику для подсчета статистики
        // Например: количество записей, посещений и т.д.

        return ResponseEntity.ok(ApiResponse.success(
                "Статистика профиля",
                Map.of(
                        "client", client.getFirstName() + " " + client.getLastName(),
                        "memberSince", client.getCreatedAt(),
                        "nextBirthday", client.getBirthDate() != null ?
                                client.getBirthDate().withYear(LocalDate.now().getYear()) : null,
                        "note", "Статистика записей будет добавлена позже"
                )
        ));
    }

    @GetMapping("/master")
    @Operation(summary = "Получить профиль мастера (публичный)")
    public ResponseEntity<ApiResponse> getMasterProfile() {
        // Ищем пользователя с ролью ADMIN
        User master = userRepository.findAll().stream()
                .filter(user -> user.getRole() == com.frolovsnails.entity.Role.ADMIN)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Мастер не найден в системе"));

        Client masterClient = clientRepository.findByUserId(master.getId()).orElse(null);

        return ResponseEntity.ok(ApiResponse.success(
                "Профиль мастера",
                Map.of(
                        "name", masterClient != null ?
                                masterClient.getFirstName() + " " + masterClient.getLastName() : "Мастер",
                        "phone", master.getPhone(),
                        "experience", "Опытный мастер маникюра",
                        "specialization", "Гелевое наращивание, дизайн ногтей, аппаратный маникюр",
                        "workingHours", "Пн-Пт: 10:00-19:00, Сб: 10:00-17:00",
                        "note", "Запись через приложение или по телефону"
                )
        ));
    }
}