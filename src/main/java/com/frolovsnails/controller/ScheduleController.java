package com.frolovsnails.controller;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.frolovsnails.dto.request.CreateScheduleBlockRequest;
import com.frolovsnails.dto.response.ApiResponse;
import com.frolovsnails.entity.AvailableDay;
import com.frolovsnails.entity.ScheduleBlock;
import com.frolovsnails.entity.Service;
import com.frolovsnails.repository.AvailableDayRepository;
import com.frolovsnails.repository.ScheduleBlockRepository;
import com.frolovsnails.repository.ServiceRepository;
import com.frolovsnails.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/schedule")
@Tag(name = "Schedule", description = "Управление расписанием и доступными днями")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final AvailableDayRepository availableDayRepository;
    private final ScheduleBlockRepository scheduleBlockRepository;
    private final ServiceRepository serviceRepository;

    // ========== ПУБЛИЧНЫЕ ЭНДПОИНТЫ (для клиентов) ==========

    @GetMapping("/available-days")
    @Operation(summary = "Получить доступные дни для записи (публичный)")
    public ResponseEntity<ApiResponse> getAvailableDays(
            @RequestParam(defaultValue = "30") int daysCount) {

        try {
            List<AvailableDay> availableDays = scheduleService.getUpcomingAvailableDays(daysCount);

            return ResponseEntity.ok(ApiResponse.success(
                    "Доступные дни для записи",
                    Map.of(
                            "availableDays", availableDays,
                            "count", availableDays.size(),
                            "daysCount", daysCount
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Ошибка получения доступных дней: " + e.getMessage())
            );
        }
    }

    @GetMapping("/availability")
    @Operation(summary = "Получить доступные слоты на конкретную дату (публичный)")
    public ResponseEntity<ApiResponse> getAvailableSlots(
            @RequestParam LocalDate date,
            @RequestParam Long serviceId) {

        try {
            // В реальном приложении здесь нужно получать услугу из БД
            // и использовать её длительность. Пока что возвращаем заглушку.

            // Для теста: предполагаем длительность 60 минут
            int durationMinutes = 60;

            // todo: REMAKE -->
            Optional<Service> service = serviceRepository.findById(serviceId);
            durationMinutes = service.get().getDurationMinutes() != null ? service.get().getDurationMinutes() : 60;

            String duratationAtString = (double)(durationMinutes/60) + " часа";

            // todo: END

            List<LocalDateTime> availableSlots = scheduleService.getAvailableSlotsForClients(date, durationMinutes);

            return ResponseEntity.ok(ApiResponse.success(
                    "Доступные слоты на " + date,
                    Map.of(
                            "date", date,
                            "availableSlots", availableSlots,
                            "count", availableSlots.size(),
                            "slotDuration", duratationAtString,
                            "note", "Запись возможна только в указанные времена"
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Ошибка получения доступного времени: " + e.getMessage())
            );
        }
    }

    // ========== АДМИН ЭНДПОИНТЫ (для мастера) ==========

    @PostMapping("/available-days") //todo: какая-то хуйня с датой из сваггера //fix: добавление Schema решило проблему
    @Operation(summary = "Добавить доступный день (только для ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> addAvailableDay(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") @Schema(type = "string", pattern = "^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$", example = "08:00") LocalTime workStart,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") @Schema(type = "string", pattern = "^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$", example = "15:00") LocalTime workEnd,
            @RequestParam(required = false) String notes) {

        try {
            AvailableDay availableDay = scheduleService.addAvailableDay(date, workStart, workEnd, notes);

            return ResponseEntity.ok(ApiResponse.success(
                    "Доступный день добавлен",
                    availableDay
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Ошибка добавления дня: " + e.getMessage())
            );
        }
    }

    @GetMapping("/admin/available-days")
    @Operation(summary = "Получить все дни расписания (только для ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> getAllAvailableDays(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        try {
            List<AvailableDay> days;

            if (startDate != null && endDate != null) {
                days = scheduleService.getAvailableDays(startDate, endDate);
            } else {
                // По умолчанию показываем ближайшие 60 дней
                LocalDate defaultStart = LocalDate.now();
                LocalDate defaultEnd = defaultStart.plusDays(60);
                days = scheduleService.getAvailableDays(defaultStart, defaultEnd);
            }

            return ResponseEntity.ok(ApiResponse.success(
                    "Дни расписания",
                    Map.of(
                            "days", days,
                            "count", days.size()
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Ошибка получения дней: " + e.getMessage())
            );
        }
    }

    @PutMapping("/available-days/{id}")
    @Operation(summary = "Обновить доступный день (только для ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> updateAvailableDay(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime workStart,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime workEnd,
            @RequestParam Boolean isAvailable,
            @RequestParam(required = false) String notes) {

        try {
            AvailableDay availableDay = scheduleService.updateAvailableDay(id, workStart, workEnd, isAvailable, notes);

            return ResponseEntity.ok(ApiResponse.success(
                    "День обновлен",
                    availableDay
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Ошибка обновления дня: " + e.getMessage())
            );
        }
    }

    @DeleteMapping("/available-days/{id}")
    @Operation(summary = "Удалить доступный день (только для ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> deleteAvailableDay(@PathVariable Long id) {
        try {
            scheduleService.deleteAvailableDay(id);
            return ResponseEntity.ok(ApiResponse.success("День удален"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Ошибка удаления дня: " + e.getMessage())
            );
        }
    }

    @GetMapping("/master/available-time")
    @Operation(summary = "Получить свободное время для ручной записи (только для ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> getMasterAvailableTime(
            @RequestParam LocalDate date,
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

    @PostMapping("/blocks")
    @Operation(summary = "Заблокировать время (отпуск, больничный) (только для ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> createScheduleBlock(@Valid @RequestBody CreateScheduleBlockRequest request) {
        try {
            ScheduleBlock block = scheduleService.blockTime(
                    request.getStartTime(),
                    request.getEndTime(),
                    request.getReason(),
                    request.getNotes()
            );

            return ResponseEntity.ok(ApiResponse.success(
                    "Время успешно заблокировано",
                    block
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Ошибка блокировки времени: " + e.getMessage())
            );
        }
    }

    @GetMapping("/blocks")
    @Operation(summary = "Получить все блокировки времени (только для ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> getScheduleBlocks(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        List<ScheduleBlock> blocks;

        if (startDate != null && endDate != null) {
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.plusDays(1).atStartOfDay();
            blocks = scheduleBlockRepository.findBlocksInRange(start, end);
        } else {
            blocks = scheduleBlockRepository.findAll();
        }

        return ResponseEntity.ok(ApiResponse.success(
                "Блокировки времени",
                Map.of(
                        "blocks", blocks,
                        "count", blocks.size()
                )
        ));
    }

    @DeleteMapping("/blocks/{id}")
    @Operation(summary = "Разблокировать время (только для ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> deleteScheduleBlock(@PathVariable Long id) {
        try {
            scheduleService.unblockTime(id);
            return ResponseEntity.ok(ApiResponse.success("Время разблокировано"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Ошибка разблокировки: " + e.getMessage())
            );
        }
    }
}