package com.frolovsnails.controller;

import com.frolovsnails.dto.request.CreateWorkSlotRequest;
import com.frolovsnails.dto.response.ApiResponse;
import com.frolovsnails.entity.SlotStatus;
import com.frolovsnails.entity.WorkSlot;
import com.frolovsnails.repository.WorkSlotRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/schedule")
@Tag(name = "Schedule", description = "Управление расписанием и рабочими слотами")
@RequiredArgsConstructor
public class ScheduleController {

    private final WorkSlotRepository workSlotRepository;

    // ========== ПУБЛИЧНЫЕ ЭНДПОИНТЫ (для клиентов) ==========

    @GetMapping("/availability")
    @Operation(summary = "Получить доступные слоты для записи")
    public ResponseEntity<ApiResponse> getAvailableSlots(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDate targetDate = date != null ? date : LocalDate.now().plusDays(1);

        List<WorkSlot> availableSlots = workSlotRepository.findByDateAndStatus(
                targetDate, SlotStatus.AVAILABLE
        );

        return ResponseEntity.ok(ApiResponse.success(
                "Доступные слоты на " + targetDate,
                Map.of(
                        "date", targetDate,
                        "availableSlots", availableSlots,
                        "count", availableSlots.size()
                )
        ));
    }

    @GetMapping("/availability/range")
    @Operation(summary = "Получить доступные слоты в диапазоне дат")
    public ResponseEntity<ApiResponse> getAvailableSlotsInRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        if (startDate.isAfter(endDate)) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Дата начала не может быть позже даты окончания")
            );
        }

        List<WorkSlot> availableSlots = workSlotRepository.findByDateBetweenAndStatus(
                startDate, endDate, SlotStatus.AVAILABLE
        );

        return ResponseEntity.ok(ApiResponse.success(
                "Доступные слоты с " + startDate + " по " + endDate,
                Map.of(
                        "startDate", startDate,
                        "endDate", endDate,
                        "availableSlots", availableSlots,
                        "count", availableSlots.size()
                )
        ));
    }

    // ========== АДМИН ЭНДПОИНТЫ (для мастера) ==========

    @GetMapping("/slots")
    @Operation(summary = "Получить все слоты (только для ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> getAllSlots(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        List<WorkSlot> slots;
        if (date != null) {
            slots = workSlotRepository.findByDate(date);
        } else {
            slots = workSlotRepository.findAll();
        }

        // Группируем по статусам для удобства
        long availableCount = slots.stream().filter(s -> s.getStatus() == SlotStatus.AVAILABLE).count();
        long bookedCount = slots.stream().filter(s -> s.getStatus() == SlotStatus.BOOKED).count();
        long blockedCount = slots.stream().filter(s -> s.getStatus() == SlotStatus.BLOCKED).count();

        return ResponseEntity.ok(ApiResponse.success(
                date != null ? "Слоты на " + date : "Все слоты",
                Map.of(
                        "slots", slots,
                        "totalCount", slots.size(),
                        "stats", Map.of(
                                "available", availableCount,
                                "booked", bookedCount,
                                "blocked", blockedCount
                        )
                )
        ));
    }

    @GetMapping("/slots/{id}")
    @Operation(summary = "Получить слот по ID (только для ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> getSlotById(@PathVariable Long id) {
        return workSlotRepository.findById(id)
                .map(slot -> ResponseEntity.ok(ApiResponse.success("Слот найден", slot)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/slots")
    @Operation(summary = "Создать рабочий слот (только для ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> createWorkSlot(@Valid @RequestBody CreateWorkSlotRequest request) {
        try {
            // Проверяем, не пересекается ли слот с существующими
            boolean exists = workSlotRepository.existsByDateAndStartTimeAndEndTime(
                    request.getDate(), request.getStartTime(), request.getEndTime()
            );

            if (exists) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error("Слот на это время уже существует")
                );
            }

            WorkSlot slot = new WorkSlot();
            slot.setDate(request.getDate());
            slot.setStartTime(request.getStartTime());
            slot.setEndTime(request.getEndTime());
            slot.setStatus(request.getStatus() != null ? request.getStatus() : SlotStatus.AVAILABLE);
            slot.setMasterNotes(request.getMasterNotes());

            WorkSlot savedSlot = workSlotRepository.save(slot);
            return ResponseEntity.ok(ApiResponse.success(
                    "Рабочий слот создан",
                    savedSlot
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Ошибка создания слота: " + e.getMessage())
            );
        }
    }

    @PostMapping("/slots/batch")
    @Operation(summary = "Создать несколько слотов пакетно (только для ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> createWorkSlotsBatch(@Valid @RequestBody List<CreateWorkSlotRequest> requests) {
        try {
            List<WorkSlot> createdSlots = requests.stream()
                    .map(request -> {
                        WorkSlot slot = new WorkSlot();
                        slot.setDate(request.getDate());
                        slot.setStartTime(request.getStartTime());
                        slot.setEndTime(request.getEndTime());
                        slot.setStatus(request.getStatus() != null ? request.getStatus() : SlotStatus.AVAILABLE);
                        slot.setMasterNotes(request.getMasterNotes());
                        return slot;
                    })
                    .toList();

            List<WorkSlot> savedSlots = workSlotRepository.saveAll(createdSlots);

            return ResponseEntity.ok(ApiResponse.success(
                    "Создано " + savedSlots.size() + " слотов",
                    savedSlots
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Ошибка пакетного создания: " + e.getMessage())
            );
        }
    }

    @PutMapping("/slots/{id}")
    @Operation(summary = "Обновить рабочий слот (только для ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> updateWorkSlot(
            @PathVariable Long id,
            @Valid @RequestBody CreateWorkSlotRequest request) {

        return workSlotRepository.findById(id)
                .map(slot -> {
                    // Нельзя изменять забронированные слоты (должны быть через отмену записи)
                    if (slot.getStatus() == SlotStatus.BOOKED) {
                        return ResponseEntity.badRequest().body(
                                ApiResponse.error("Нельзя изменять забронированный слот. Сначала отмените запись.")
                        );
                    }

                    slot.setDate(request.getDate());
                    slot.setStartTime(request.getStartTime());
                    slot.setEndTime(request.getEndTime());
                    slot.setStatus(request.getStatus() != null ? request.getStatus() : slot.getStatus());
                    slot.setMasterNotes(request.getMasterNotes());

                    WorkSlot updatedSlot = workSlotRepository.save(slot);
                    return ResponseEntity.ok(ApiResponse.success(
                            "Слот обновлен",
                            updatedSlot
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/slots/{id}/block")
    @Operation(summary = "Заблокировать слот (отпуск/болезнь) (только для ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> blockSlot(@PathVariable Long id) {
        return workSlotRepository.findById(id)
                .map(slot -> {
                    if (slot.getStatus() == SlotStatus.BOOKED) {
                        return ResponseEntity.badRequest().body(
                                ApiResponse.error("Нельзя заблокировать забронированный слот. Сначала отмените запись.")
                        );
                    }

                    slot.setStatus(SlotStatus.BLOCKED);
                    workSlotRepository.save(slot);
                    return ResponseEntity.ok(ApiResponse.success("Слот заблокирован"));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/slots/{id}/unblock")
    @Operation(summary = "Разблокировать слот (только для ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> unblockSlot(@PathVariable Long id) {
        return workSlotRepository.findById(id)
                .map(slot -> {
                    slot.setStatus(SlotStatus.AVAILABLE);
                    workSlotRepository.save(slot);
                    return ResponseEntity.ok(ApiResponse.success("Слот разблокирован"));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/slots/{id}")
    @Operation(summary = "Удалить слот (только для ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> deleteSlot(@PathVariable Long id) {
        return workSlotRepository.findById(id)
                .map(slot -> {
                    if (slot.getStatus() == SlotStatus.BOOKED) {
                        return ResponseEntity.badRequest().body(
                                ApiResponse.error("Нельзя удалить забронированный слот. Сначала отмените запись.")
                        );
                    }

                    workSlotRepository.delete(slot);
                    return ResponseEntity.ok(ApiResponse.success("Слот удален"));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}