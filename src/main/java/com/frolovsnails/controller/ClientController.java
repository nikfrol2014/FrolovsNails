package com.frolovsnails.controller;

import com.frolovsnails.dto.request.CreateMasterAppointmentRequest;
import com.frolovsnails.dto.request.UpdateClientRequest;
import com.frolovsnails.dto.response.ApiResponse;
import com.frolovsnails.dto.response.AppointmentResponse;
import com.frolovsnails.dto.response.ClientDetailsResponse;
import com.frolovsnails.entity.Appointment;
import com.frolovsnails.entity.AppointmentStatus;
import com.frolovsnails.entity.Client;
import com.frolovsnails.entity.User;
import com.frolovsnails.mapper.AppointmentMapper;
import com.frolovsnails.repository.AppointmentRepository;
import com.frolovsnails.repository.ClientRepository;
import com.frolovsnails.repository.UserRepository;
import com.frolovsnails.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/clients")
@Tag(name = "Clients", description = "Управление клиентами")
@RequiredArgsConstructor
public class ClientController {

    private final ClientRepository clientRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final AppointmentMapper appointmentMapper;
    private final AppointmentService appointmentService;

    @GetMapping("/{clientId}/details")
    @Operation(summary = "Получить детальную информацию о клиенте")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> getClientDetails(@PathVariable Long clientId) {

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Клиент не найден"));

        // Все записи клиента
        List<Appointment> allAppointments = appointmentRepository.findByClientId(clientId);

        // Статистика
        ClientDetailsResponse.ClientStats stats = calculateClientStats(client, allAppointments);

        // Последние 5 записей
        List<AppointmentResponse> recentAppointments = allAppointments.stream()
                .sorted((a1, a2) -> a2.getStartTime().compareTo(a1.getStartTime()))
                .limit(5)
                .map(appointmentMapper::toResponse)
                .toList();

        // Будущие записи
        List<AppointmentResponse> upcomingAppointments = allAppointments.stream()
                .filter(a -> a.getStartTime().isAfter(LocalDateTime.now())
                        && a.getStatus() != AppointmentStatus.CANCELLED
                        && a.getStatus() != AppointmentStatus.COMPLETED)
                .sorted((a1, a2) -> a1.getStartTime().compareTo(a2.getStartTime()))
                .map(appointmentMapper::toResponse)
                .toList();

        ClientDetailsResponse response = ClientDetailsResponse.builder()
                .client(mapToClientInfo(client))
                .stats(stats)
                .recentAppointments(recentAppointments)
                .upcomingAppointments(upcomingAppointments)
                .build();

        return ResponseEntity.ok(ApiResponse.success("Детали клиента", response));
    }

    private ClientDetailsResponse.ClientInfo mapToClientInfo(Client client) {
        return ClientDetailsResponse.ClientInfo.builder()
                .id(client.getId())
                .firstName(client.getFirstName())
                .lastName(client.getLastName())
                .phone(client.getUser().getPhone())
                .birthDate(client.getBirthDate())
                .notes(client.getNotes())
                .registeredAt(client.getCreatedAt())
//                .isVip(client.getIsVip() != null ? client.getIsVip() : false)
                .build();
    }

    private ClientDetailsResponse.ClientStats calculateClientStats(Client client, List<Appointment> appointments) {
        if (appointments.isEmpty()) {
            return ClientDetailsResponse.ClientStats.builder()
                    .totalVisits(0)
                    .cancelledVisits(0)
                    .totalSpent(BigDecimal.ZERO)
                    .build();
        }

        // Статистика по статусам
        int completed = (int) appointments.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED).count();
        int cancelled = (int) appointments.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.CANCELLED).count();

        // Сумма потраченного
        BigDecimal totalSpent = appointments.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED)
                .map(a -> a.getService().getPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Любимая услуга
        Map<String, Long> serviceCount = appointments.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED)
                .collect(Collectors.groupingBy(
                        a -> a.getService().getName(),
                        Collectors.counting()
                ));

        String favoriteService = serviceCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Нет данных");

        Long favoriteCount = serviceCount.getOrDefault(favoriteService, 0L);

        // Первый и последний визит
        Optional<LocalDateTime> firstVisit = appointments.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED)
                .map(Appointment::getStartTime)
                .min(LocalDateTime::compareTo);

        Optional<LocalDateTime> lastVisit = appointments.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED)
                .map(Appointment::getStartTime)
                .max(LocalDateTime::compareTo);

        return ClientDetailsResponse.ClientStats.builder()
                .totalVisits(appointments.size())
                .cancelledVisits(cancelled)
                .noShowVisits(0) // Пока не реализовано
                .totalSpent(totalSpent)
                .averageBill(completed > 0 ?
                        totalSpent.divide(BigDecimal.valueOf(completed), RoundingMode.HALF_UP) :
                        BigDecimal.ZERO)
                .firstVisitDate(firstVisit.map(LocalDateTime::toLocalDate).orElse(null))
                .lastVisitDate(lastVisit.map(LocalDateTime::toLocalDate).orElse(null))
                .favoriteService(favoriteService)
                .favoriteServiceCount(favoriteCount.intValue())
                .attendanceRate(calculateAttendanceRate(appointments))
                .build();
    }

    private Double calculateAttendanceRate(List<Appointment> appointments) {
        if (appointments.isEmpty()) return 0.0;

        long total = appointments.size();
        long completed = appointments.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED).count();

        return (double) completed / total * 100;
    }

    @PutMapping("/{clientId}")
    @Operation(summary = "Обновить данные клиента")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> updateClient(
            @PathVariable Long clientId,
            @Valid @RequestBody UpdateClientRequest request) {

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Клиент не найден"));

        // Обновляем данные
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

        // Обновляем телефон в User
        if (request.getPhone() != null) {
            User user = client.getUser();
            user.setPhone(request.getPhone());
            userRepository.save(user);
        }

        clientRepository.save(client);

        return ResponseEntity.ok(ApiResponse.success("✅ Данные клиента обновлены"));
    }

    @PostMapping("/{clientId}/appointments")
    @Operation(summary = "Создать запись для конкретного клиента")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> createAppointmentForClient(
            @PathVariable Long clientId,
            @Valid @RequestBody CreateMasterAppointmentRequest request) {

        // Устанавливаем ID клиента из пути
        request.setClientId(clientId);

        Appointment appointment = appointmentService.createMasterAppointment(request);

        return ResponseEntity.ok(ApiResponse.success(
                "✅ Запись создана для клиента",
                appointmentMapper.toResponse(appointment)
        ));
    }
}