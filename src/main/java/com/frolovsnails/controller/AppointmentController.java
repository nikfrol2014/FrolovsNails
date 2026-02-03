package com.frolovsnails.controller;

import com.frolovsnails.dto.request.CreateAppointmentRequest;
import com.frolovsnails.dto.request.CreateMasterAppointmentRequest;
import com.frolovsnails.dto.request.UpdateAppointmentStatusRequest;
import com.frolovsnails.dto.response.ApiResponse;
import com.frolovsnails.dto.response.AppointmentResponse;
import com.frolovsnails.entity.*;
import com.frolovsnails.mapper.AppointmentMapper;
import com.frolovsnails.repository.AppointmentRepository;
import com.frolovsnails.repository.ServiceRepository;
import com.frolovsnails.repository.UserRepository;
import com.frolovsnails.service.AppointmentService;
import com.frolovsnails.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/appointments")
@Tag(name = "Appointments", description = "Управление записями")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final ScheduleService scheduleService;
    private final ServiceRepository serviceRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentMapper appointmentMapper;
    private final UserRepository userRepository;

    // ========== КЛИЕНТСКИЕ ЭНДПОИНТЫ ==========

    @GetMapping("/client/available-slots")
    @Operation(summary = "Получить доступные слоты для записи (для клиентов)")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ApiResponse> getClientAvailableSlots(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam Long serviceId) {

        try {
            Service service = serviceRepository.findById(serviceId)
                    .orElseThrow(() -> new RuntimeException("Услуга не найдена"));

            List<LocalDateTime> slots = scheduleService.getAvailableSlotsForClients(
                    date, service.getDurationMinutes());

            return ResponseEntity.ok(ApiResponse.success(
                    "Доступные слоты на " + date,
                    Map.of(
                            "date", date,
                            "service", Map.of(
                                    "id", service.getId(),
                                    "name", service.getName(),
                                    "duration", service.getDurationMinutes()
                            ),
                            "slots", slots.stream()
                                    .map(LocalDateTime::toString)
                                    .toList(),
                            "count", slots.size(),
                            "slotDuration", "2.5 часа",
                            "note", "Запись возможна только в указанные времена"
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Ошибка получения слотов: " + e.getMessage())
            );
        }
    }

    @PostMapping("/client")
    @Operation(summary = "Создать запись через систему (для клиентов)")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ApiResponse> createClientAppointment(@Valid @RequestBody CreateAppointmentRequest request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String phone = auth.getName();

            Appointment appointment = appointmentService.createClientAppointment(phone, request);

            return ResponseEntity.ok(ApiResponse.success(
                    "✅ Запись создана успешно!",
                    Map.of(
                            "appointment", appointmentMapper.toResponse(appointment),
                            "isManual", false,
                            "note", "Запись создана через систему"
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("❌ Ошибка создания записи: " + e.getMessage())
            );
        }
    }

    @GetMapping("/my")
    @Operation(summary = "Получить мои записи (для клиентов)")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ApiResponse> getMyAppointments(
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String phone = auth.getName();

        List<Appointment> appointments;

        if (status != null && date != null) {
            appointments = appointmentService.getClientAppointmentsByStatusAndDate(phone, status, date);
        } else if (status != null) {
            appointments = appointmentService.getClientAppointmentsByStatus(phone, status);
        } else if (date != null) {
            appointments = appointmentService.getClientAppointmentsByDate(phone, date);
        } else {
            appointments = appointmentService.getClientAppointments(phone);
        }

        List<AppointmentResponse> appointmentResponses = appointments.stream()
                .map(appointmentMapper::toResponse)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(
                "Найдено записей: " + appointments.size(),
                Map.of(
                        "appointments", appointmentResponses,
                        "count", appointments.size()
                )
        ));
    }

    @GetMapping("/my/{id}")
    @Operation(summary = "Получить мою запись по ID (для клиентов)")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ApiResponse> getMyAppointmentById(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String phone = auth.getName();

        return appointmentService.getClientAppointmentById(phone, id)
                .map(appointment -> ResponseEntity.ok(
                        ApiResponse.success("Запись найдена", appointmentMapper.toResponse(appointment))))
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/my/{id}/cancel")
    @Operation(summary = "Отменить свою запись (для клиентов)")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ApiResponse> cancelMyAppointment(@PathVariable Long id) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String phone = auth.getName();

            Appointment appointment = appointmentService.cancelClientAppointment(phone, id);

            return ResponseEntity.ok(ApiResponse.success(
                    "✅ Запись отменена",
                    Map.of(
                            "appointment", appointmentMapper.toResponse(appointment),
                            "status", appointment.getStatus()
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("❌ Ошибка отмены записи: " + e.getMessage())
            );
        }
    }

    // ========== МАСТЕРСКИЕ ЭНДПОИНТЫ ==========

    @GetMapping("/master/available-time")
    @Operation(summary = "Получить свободное время для ручной записи (только для ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> getMasterAvailableTime(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false, defaultValue = "30") Integer minDuration) {

        try {
            List<ScheduleService.TimeRange> availableRanges =
                    scheduleService.getAvailableTimeRangesForMaster(date, minDuration);

            return ResponseEntity.ok(ApiResponse.success(
                    "Свободное время на " + date,
                    Map.of(
                            "date", date,
                            "minDuration", minDuration + " минут",
                            "availableRanges", availableRanges.stream()
                                    .map(range -> Map.of(
                                            "start", range.getStartTime().toString(),
                                            "end", range.getEndTime().toString(),
                                            "duration", range.getDurationMinutes() + " минут"
                                    ))
                                    .toList(),
                            "count", availableRanges.size(),
                            "note", "Мастер может записать в любое свободное время"
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Ошибка получения свободного времени: " + e.getMessage())
            );
        }
    }

    @PostMapping("/master")
    @Operation(summary = "Создать ручную запись (только для ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> createMasterAppointment(@Valid @RequestBody CreateMasterAppointmentRequest request) {
        try {
            Appointment appointment = appointmentService.createMasterAppointment(request);

            return ResponseEntity.ok(ApiResponse.success(
                    "✅ Ручная запись создана",
                    Map.of(
                            "appointment", appointmentMapper.toResponse(appointment),
                            "isManual", true,
                            "note", "Запись создана мастером вручную"
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("❌ Ошибка создания ручной записи: " + e.getMessage())
            );
        }
    }

    // ========== АДМИНСКИЕ ЭНДПОИНТЫ ==========

    @GetMapping
    @Operation(summary = "Получить все записи (только для ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> getAllAppointments(
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long clientId) {

        List<Appointment> appointments;

        if (status != null && date != null) {
            appointments = appointmentService.getAppointmentsByDateAndStatus(date, status);
        } else if (status != null) {
            appointments = appointmentService.getAppointmentsByStatus(status);
        } else if (date != null) {
            appointments = appointmentService.getAppointmentsByDate(date);
        } else {
            appointments = appointmentService.getAllAppointments();
        }

        // Фильтрация по clientId если указан
        if (clientId != null) {
            appointments = appointments.stream()
                    .filter(a -> a.getClient().getId().equals(clientId))
                    .toList();
        }

        List<AppointmentResponse> appointmentResponses = appointments.stream()
                .map(appointmentMapper::toResponse)
                .toList();

        // Статистика
        long createdCount = appointments.stream().filter(a -> a.getStatus() == AppointmentStatus.CREATED).count();
        long pendingCount = appointments.stream().filter(a -> a.getStatus() == AppointmentStatus.PENDING).count();
        long confirmedCount = appointments.stream().filter(a -> a.getStatus() == AppointmentStatus.CONFIRMED).count();
        long cancelledCount = appointments.stream().filter(a -> a.getStatus() == AppointmentStatus.CANCELLED).count();
        long completedCount = appointments.stream().filter(a -> a.getStatus() == AppointmentStatus.COMPLETED).count();

        return ResponseEntity.ok(ApiResponse.success(
                "Всего записей: " + appointments.size(),
                Map.of(
                        "appointments", appointmentResponses,
                        "total", appointments.size(),
                        "stats", Map.of(
                                "created", createdCount,
                                "pending", pendingCount,
                                "confirmed", confirmedCount,
                                "cancelled", cancelledCount,
                                "completed", completedCount
                        )
                )
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить запись по ID (только для ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> getAppointmentById(@PathVariable Long id) {
        return appointmentRepository.findById(id)
                .map(appointment -> ResponseEntity.ok(
                        ApiResponse.success("Запись найдена", appointmentMapper.toResponse(appointment))))
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Изменить статус записи (только для ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> updateAppointmentStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAppointmentStatusRequest request) {

        try {
            Appointment appointment = appointmentService.updateAppointmentStatus(id, request);

            String message = switch (appointment.getStatus()) {
                case PENDING -> "✅ Запись переведена в ожидание подтверждения";
                case CONFIRMED -> "✅ Запись подтверждена!";
                case CANCELLED -> "✅ Запись отменена";
                case COMPLETED -> "✅ Запись отмечена как выполненная";
                default -> "Статус обновлен";
            };

            return ResponseEntity.ok(ApiResponse.success(
                    message,
                    Map.of(
                            "appointment", appointmentMapper.toResponse(appointment),
                            "newStatus", appointment.getStatus()
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("❌ Ошибка обновления статуса: " + e.getMessage())
            );
        }
    }

    @PatchMapping("/{id}/reschedule")
    @Operation(summary = "Перенести запись на другое время (только для ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> rescheduleAppointment(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime newStartTime) {

        try {
            Appointment appointment = appointmentService.rescheduleAppointment(id, newStartTime);

            return ResponseEntity.ok(ApiResponse.success(
                    "✅ Запись перенесена успешно",
                    Map.of(
                            "appointment", appointmentMapper.toResponse(appointment),
                            "oldTime", "Перенесена",
                            "newTime", appointment.getStartTime()
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("❌ Ошибка переноса записи: " + e.getMessage())
            );
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить запись (только для ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> deleteAppointment(@PathVariable Long id) {
        try {
            appointmentService.deleteAppointment(id);
            return ResponseEntity.ok(ApiResponse.success("✅ Запись удалена"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("❌ Ошибка удаления записи: " + e.getMessage())
            );
        }
    }

    @GetMapping("/stats/daily")
    @Operation(summary = "Статистика записей по дням (только для ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> getDailyStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        // Простая реализация - можно улучшить
        List<Appointment> allAppointments = appointmentRepository.findAll();

        List<Appointment> filteredAppointments = allAppointments.stream()
                .filter(a -> {
                    LocalDate appointmentDate = a.getStartTime().toLocalDate();
                    return !appointmentDate.isBefore(startDate) && !appointmentDate.isAfter(endDate);
                })
                .toList();

        // Группировка по дням
        Map<LocalDate, Long> dailyCount = filteredAppointments.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        a -> a.getStartTime().toLocalDate(),
                        java.util.stream.Collectors.counting()
                ));

        return ResponseEntity.ok(ApiResponse.success(
                "Статистика с " + startDate + " по " + endDate,
                Map.of(
                        "period", Map.of("start", startDate, "end", endDate),
                        "totalAppointments", filteredAppointments.size(),
                        "dailyStats", dailyCount
                )
        ));
    }

    // ========== ОБЩИЕ ЭНДПОИНТЫ ==========

    @GetMapping("/{id}/details")
    @Operation(summary = "Получить детали записи по ID")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> getAppointmentDetails(@PathVariable Long id) {
        return appointmentRepository.findById(id)
                .map(appointment -> {
                    // Проверка прав: клиент может видеть только свои записи
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    String phone = auth.getName();
                    User user = userRepository.findByPhone(phone)
                            .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

                    if (user.getRole() == Role.CLIENT &&
                            !appointment.getClient().getUser().getId().equals(user.getId())) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(ApiResponse.error("Доступ запрещен"));
                    }

                    return ResponseEntity.ok(
                            ApiResponse.success("Запись найдена", appointmentMapper.toResponse(appointment))
                    );
                })
                .orElse(ResponseEntity.notFound().build());
    }
}