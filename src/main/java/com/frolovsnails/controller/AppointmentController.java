package com.frolovsnails.controller;

import com.frolovsnails.dto.request.CreateAppointmentRequest;
import com.frolovsnails.dto.request.UpdateAppointmentStatusRequest;
import com.frolovsnails.dto.response.ApiResponse;
import com.frolovsnails.dto.response.AppointmentResponse;
import com.frolovsnails.entity.*;
import com.frolovsnails.mapper.AppointmentMapper;
import com.frolovsnails.repository.*;
import com.frolovsnails.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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
    private final AppointmentRepository appointmentRepository;
    private final ClientRepository clientRepository;
    private final ServiceRepository serviceRepository;
    private final WorkSlotRepository workSlotRepository;
    private final UserRepository userRepository;
    private final AppointmentMapper appointmentMapper;

    // ========== КЛИЕНТСКИЕ ЭНДПОИНТЫ ==========

    @PostMapping
    @Operation(summary = "Создать новую запись (для клиентов)")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ApiResponse> createAppointment(@Valid @RequestBody CreateAppointmentRequest request) {
        try {
            // Получаем текущего пользователя
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String phone = auth.getName();

            Appointment appointment = appointmentService.createAppointment(phone, request);

            return ResponseEntity.ok(ApiResponse.success(
                    "✅ Запись создана успешно! Статус: " + appointment.getStatus(),
                    Map.of(
                            "appointment", appointment,
                            "nextStep", "Ожидайте подтверждения от мастера"
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

        // Преобразуем в DTO
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
                .map(appointment -> ResponseEntity.ok(ApiResponse.success("Запись найдена", appointment)))
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
                            "appointment", appointment,
                            "status", appointment.getStatus(),
                            "slotStatus", "Слот освобожден"
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("❌ Ошибка отмены записи: " + e.getMessage())
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
        } else if (clientId != null) {
            // Для конкретного клиента
            appointments = appointmentRepository.findByClientId(clientId);
        } else {
            appointments = appointmentService.getAllAppointments();
        }

        // Преобразуем в DTO
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
                        "appointments", appointments,
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
                .map(appointment -> ResponseEntity.ok(ApiResponse.success("Запись найдена", appointment)))
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
                case CONFIRMED -> "✅ Запись подтверждена! Уведомление отправлено клиенту";
                case CANCELLED -> "✅ Запись отменена. Слот освобожден";
                case COMPLETED -> "✅ Запись отмечена как выполненная";
                default -> "Статус обновлен";
            };

            return ResponseEntity.ok(ApiResponse.success(
                    message,
                    Map.of(
                            "appointment", appointment,
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
    @Operation(summary = "Перенести запись на другой слот (только для ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> rescheduleAppointment(
            @PathVariable Long id,
            @RequestParam Long newSlotId) {

        try {
            Appointment appointment = appointmentService.rescheduleAppointment(id, newSlotId);

            return ResponseEntity.ok(ApiResponse.success(
                    "✅ Запись перенесена успешно",
                    Map.of(
                            "appointment", appointment,
                            "newSlot", appointment.getWorkSlot(),
                            "oldSlotFreed", true
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

        List<Appointment> appointments = appointmentRepository.findByCreatedAtBetween(
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay()
        );

        // Группировка по дням
        Map<LocalDate, Long> dailyCount = appointments.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        a -> a.getCreatedAt().toLocalDate(),
                        java.util.stream.Collectors.counting()
                ));

        return ResponseEntity.ok(ApiResponse.success(
                "Статистика с " + startDate + " по " + endDate,
                Map.of(
                        "period", Map.of("start", startDate, "end", endDate),
                        "totalAppointments", appointments.size(),
                        "dailyStats", dailyCount,
                        "revenueEstimate", appointments.stream()
                                .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED)
                                .mapToDouble(a -> a.getService().getPrice().doubleValue())
                                .sum()
                )
        ));
    }
}