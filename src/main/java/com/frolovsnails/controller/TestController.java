package com.frolovsnails.controller;

import com.frolovsnails.dto.request.CreateAppointmentRequest;
import com.frolovsnails.entity.*;
import com.frolovsnails.repository.*;
import com.frolovsnails.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

@RestController
@RequestMapping("/api/test")
@Tag(name = "Test Controller", description = "Тестовые эндпоинты")
@RequiredArgsConstructor
public class TestController {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final ServiceRepository serviceRepository;
//    private final WorkingScheduleRepository workingScheduleRepository;
    private final ScheduleBlockRepository scheduleBlockRepository;
    private final AppointmentRepository appointmentRepository;
    private final AvailableDayRepository availableDayRepository;
    private final AppointmentService appointmentService;

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
                "workingSchedule", availableDayRepository,
                 "scheduleBlock", scheduleBlockRepository,
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

    @PostMapping("/create-test-available-days")
    @Operation(summary = "Создать тестовые доступные дни")
    public ResponseEntity<Map<String, Object>> createTestAvailableDays() {
        try {
            // Удаляем старые доступные дни если есть
            availableDayRepository.deleteAll();

            // Создаем тестовые доступные дни на ближайшие 7 дней
            List<AvailableDay> days = new ArrayList<>();
            LocalDate today = LocalDate.now();

            for (int i = 1; i <= 7; i++) {
                LocalDate date = today.plusDays(i);

                // Делаем выходными субботу и воскресенье
                boolean isWeekend = date.getDayOfWeek() == DayOfWeek.SATURDAY
                        || date.getDayOfWeek() == DayOfWeek.SUNDAY;

                AvailableDay day = new AvailableDay();
                day.setAvailableDate(date);
                day.setWorkStart(LocalTime.of(10, 0));
                day.setWorkEnd(LocalTime.of(19, 0));
                day.setIsAvailable(!isWeekend);
                day.setNotes(isWeekend ? "Выходной" : "Рабочий день");

                days.add(day);
            }

            availableDayRepository.saveAll(days);

            // Создаем тестовую блокировку
            ScheduleBlock block = new ScheduleBlock();
            block.setStartTime(LocalDateTime.now().plusDays(3).withHour(14).withMinute(0));
            block.setEndTime(LocalDateTime.now().plusDays(3).withHour(16).withMinute(0));
            block.setReason("MEETING");
            block.setNotes("Встреча с поставщиком");
            block.setIsBlocked(true);
            scheduleBlockRepository.save(block);

            return ResponseEntity.ok(Map.of(
                    "message", "✅ Тестовые доступные дни созданы",
                    "createdDays", days.size(),
                    "nextWeekAvailable", days.stream()
                            .filter(AvailableDay::getIsAvailable)
                            .map(d -> d.getAvailableDate().toString())
                            .toList(),
                    "weekendDays", days.stream()
                            .filter(d -> !d.getIsAvailable())
                            .map(d -> d.getAvailableDate().toString())
                            .toList(),
                    "blockedTime", block.getStartTime().toLocalDate().toString() + " 14:00-16:00"
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/create-test-appointments")
    @Operation(summary = "Создать тестовые записи (новый формат)")
    public ResponseEntity<Map<String, Object>> createTestAppointments() {
        try {
            // Удаляем старые записи
            appointmentRepository.deleteAll();

            // Получаем тестовых клиентов
            Optional<Client> client1 = clientRepository.findByUserPhone("12345");
            Optional<Client> client2 = clientRepository.findByUserPhone("123456");

            List<Service> services = serviceRepository.findAll();

            if (services.isEmpty() || (!client1.isPresent() && !client2.isPresent())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Недостаточно данных для создания записей, или тут захардкожен номер телефона, а владельца его уже нет ))))"
                ));
            }

            List<Appointment> appointments = new ArrayList<>();

            // Запись 1: завтра в 11:00
            if (client1.isPresent() && !services.isEmpty()) {
                Appointment appointment1 = new Appointment();
                appointment1.setClient(client1.get());
                appointment1.setService(services.get(0));
                appointment1.setStartTime(LocalDateTime.now().plusDays(1).withHour(11).withMinute(0));
                appointment1.setEndTime(appointment1.getStartTime().plusMinutes(services.get(0).getDurationMinutes()));
                appointment1.setStatus(AppointmentStatus.CONFIRMED);
                appointment1.setClientNotes("Тестовая запись 1");
                appointments.add(appointment1);
            }

            // Запись 2: послезавтра в 14:30
            if (client2.isPresent() && services.size() >= 2) {
                Appointment appointment2 = new Appointment();
                appointment2.setClient(client2.get());
                appointment2.setService(services.get(1));
                appointment2.setStartTime(LocalDateTime.now().plusDays(2).withHour(14).withMinute(30));
                appointment2.setEndTime(appointment2.getStartTime().plusMinutes(services.get(1).getDurationMinutes()));
                appointment2.setStatus(AppointmentStatus.PENDING);
                appointment2.setClientNotes("Тестовая запись 2");
                appointments.add(appointment2);
            }

            appointmentRepository.saveAll(appointments);

            return ResponseEntity.ok(Map.of(
                    "message", "✅ Тестовые записи созданы",
                    "count", appointments.size(),
                    "appointments", appointments.stream()
                            .map(a -> Map.of(
                                    "id", a.getId(),
                                    "client", a.getClient().getFirstName(),
                                    "service", a.getService().getName(),
                                    "time", a.getStartTime().toString()
                            ))
                            .toList()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @Operation(summary = "тестирование двойной записи на одно время")
    @GetMapping("/test/race-condition")
    public String testRaceCondition() {
        // Эмуляция двух одновременных запросов
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Callable<Appointment> task1 = () ->
                appointmentService.createClientAppointment("12345",
                        new CreateAppointmentRequest(1L, LocalDateTime.now().plusDays(1).withHour(15), ""));

        Callable<Appointment> task2 = () ->
                appointmentService.createClientAppointment("123456",
                        new CreateAppointmentRequest(1L, LocalDateTime.now().plusDays(1).withHour(15), ""));

        Future<Appointment> future1 = executor.submit(task1);
        Future<Appointment> future2 = executor.submit(task2);

        try {
            Appointment result1 = future1.get(1, TimeUnit.SECONDS);
            Appointment result2 = future2.get(1, TimeUnit.SECONDS);
            return "Обе записи созданы? Должна была упасть одна!";
        } catch (Exception e) {
            return "Одна запись упала с ошибкой: " + e.getMessage(); // Это ожидаемо
        }
    }
}