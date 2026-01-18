package com.frolovsnails.controller;

import com.frolovsnails.entity.*;
import com.frolovsnails.repository.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    @PostMapping("/create-test-services")
    @Operation(summary = "Создать тестовые услуги (публичный)")
    public ResponseEntity<Map<String, Object>> createTestServices() {
        try {
            // Проверяем, есть ли уже услуги
            if (serviceRepository.count() > 0) {
                return ResponseEntity.ok(Map.of(
                        "message", "Услуги уже существуют",
                        "count", serviceRepository.count()
                ));
            }

            // Создаем тестовые услуги
            List<Service> services = List.of(
                    createService("Маникюр классический", "Классический маникюр с покрытием", 90, 1500, "Маникюр"),
                    createService("Маникюр аппаратный", "Аппаратный маникюр", 120, 2000, "Маникюр"),
                    createService("Педикюр классический", "Классический педикюр", 120, 2000, "Педикюр"),
                    createService("Педикюр аппаратный", "Аппаратный педикюр", 150, 2500, "Педикюр"),
                    createService("Наращивание ногтей", "Наращивание гелем", 180, 3000, "Маникюр"),
                    createService("Дизайн ногтей", "Художественный дизайн", 60, 1000, "Маникюр"),
                    createService("Снятие покрытия", "Снятие гель-лака", 30, 500, "Маникюр"),
                    createService("SPA-уход для рук", "SPA процедура для рук", 90, 1800, "Маникюр"),
                    createService("SPA-уход для ног", "SPA процедура для ног", 120, 2200, "Педикюр")
            );

            serviceRepository.saveAll(services);

            return ResponseEntity.ok(Map.of(
                    "message", "✅ Тестовые услуги созданы",
                    "count", services.size(),
                    "categories", List.of("Маникюр", "Педикюр")
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    private Service createService(String name, String description, int duration, int price, String category) {
        Service service = new Service();
        service.setName(name);
        service.setDescription(description);
        service.setDurationMinutes(duration);
        service.setPrice(BigDecimal.valueOf(price));
        service.setCategory(category);
        service.setIsActive(true);
        return service;
    }

    @PostMapping("/create-test-slots")
    @Operation(summary = "Создать тестовые рабочие слоты")
    public ResponseEntity<Map<String, Object>> createTestSlots() {
        try {
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            LocalDate dayAfterTomorrow = LocalDate.now().plusDays(2);

            // Проверяем, есть ли уже слоты на эти даты
            if (!workSlotRepository.findByDate(tomorrow).isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "message", "Слоты уже существуют на " + tomorrow
                ));
            }

            // Создаем слоты на завтра
            List<WorkSlot> slots = List.of(
                    createSlot(tomorrow, LocalTime.of(10, 0), LocalTime.of(11, 30), SlotStatus.AVAILABLE, "Утро"),
                    createSlot(tomorrow, LocalTime.of(12, 0), LocalTime.of(13, 30), SlotStatus.AVAILABLE, "Обед"),
                    createSlot(tomorrow, LocalTime.of(14, 0), LocalTime.of(15, 30), SlotStatus.AVAILABLE, "День"),
                    createSlot(tomorrow, LocalTime.of(16, 0), LocalTime.of(17, 30), SlotStatus.BLOCKED, "Встреча"),

                    // Слоты на послезавтра
                    createSlot(dayAfterTomorrow, LocalTime.of(9, 0), LocalTime.of(10, 30), SlotStatus.AVAILABLE, "Раннее утро"),
                    createSlot(dayAfterTomorrow, LocalTime.of(11, 0), LocalTime.of(12, 30), SlotStatus.AVAILABLE, null),
                    createSlot(dayAfterTomorrow, LocalTime.of(13, 0), LocalTime.of(14, 30), SlotStatus.AVAILABLE, null)
            );

            workSlotRepository.saveAll(slots);

            return ResponseEntity.ok(Map.of(
                    "message", "✅ Тестовые слоты созданы",
                    "count", slots.size(),
                    "dates", List.of(tomorrow, dayAfterTomorrow),
                    "note", "Один слот заблокирован (BLOCKED) как пример"
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    private WorkSlot createSlot(LocalDate date, LocalTime start, LocalTime end, SlotStatus status, String notes) {
        WorkSlot slot = new WorkSlot();
        slot.setDate(date);
        slot.setStartTime(start);
        slot.setEndTime(end);
        slot.setStatus(status);
        slot.setMasterNotes(notes);
        return slot;
    }

    @PostMapping("/create-test-appointments")
    @Operation(summary = "Создать тестовые записи")
    public ResponseEntity<Map<String, Object>> createTestAppointments() {
        try {
            // Проверяем, есть ли уже записи
            if (appointmentRepository.count() > 0) {
                return ResponseEntity.ok(Map.of(
                        "message", "Записи уже существуют",
                        "count", appointmentRepository.count()
                ));
            }

            // Получаем тестовых клиентов и услуги
            Optional<Client> client1 = clientRepository.findByUserPhone("+79001112233");
            Optional<Client> client2 = clientRepository.findByUserPhone("+79991234567");

            List<Service> services = serviceRepository.findAll();
            List<WorkSlot> slots = workSlotRepository.findAll();

            if (services.isEmpty() || slots.isEmpty() || (!client1.isPresent() && !client2.isPresent())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Недостаточно данных для создания записей",
                        "services", services.size(),
                        "slots", slots.size(),
                        "clients", (client1.isPresent() ? 1 : 0) + (client2.isPresent() ? 1 : 0)
                ));
            }

            // Создаем тестовые записи
            List<Appointment> appointments = List.of();

            if (client1.isPresent() && services.size() >= 1 && slots.size() >= 1) {
                Appointment appointment1 = new Appointment();
                appointment1.setClient(client1.get());
                appointment1.setService(services.get(0));
                appointment1.setWorkSlot(slots.get(0));
                appointment1.setStatus(AppointmentStatus.CONFIRMED);
                appointment1.setClientNotes("Тестовая запись 1");
                appointments.add(appointment1);

                // Обновляем статус слота
                slots.get(0).setStatus(SlotStatus.BOOKED);
                workSlotRepository.save(slots.get(0));
            }

            if (client2.isPresent() && services.size() >= 2 && slots.size() >= 2) {
                Appointment appointment2 = new Appointment();
                appointment2.setClient(client2.get());
                appointment2.setService(services.get(1));
                appointment2.setWorkSlot(slots.get(1));
                appointment2.setStatus(AppointmentStatus.PENDING);
                appointment2.setClientNotes("Тестовая запись 2");
                appointments.add(appointment2);

                // Обновляем статус слота
                slots.get(1).setStatus(SlotStatus.BOOKED);
                workSlotRepository.save(slots.get(1));
            }

            appointmentRepository.saveAll(appointments);

            return ResponseEntity.ok(Map.of(
                    "message", "✅ Тестовые записи созданы",
                    "count", appointments.size(),
                    "statuses", appointments.stream()
                            .map(a -> a.getStatus().toString())
                            .toList()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }
}