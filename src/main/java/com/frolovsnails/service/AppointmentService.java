package com.frolovsnails.service;

import com.frolovsnails.dto.request.CreateAppointmentRequest;
import com.frolovsnails.dto.request.CreateMasterAppointmentRequest;
import com.frolovsnails.dto.request.UpdateAppointmentStatusRequest;
import com.frolovsnails.dto.response.AppointmentResponse;
import com.frolovsnails.entity.*;
import com.frolovsnails.mapper.AppointmentMapper;
import com.frolovsnails.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import com.frolovsnails.entity.Appointment;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final ClientRepository clientRepository;
    private final ServiceRepository serviceRepository;
    private final UserRepository userRepository;
    private final AppointmentMapper appointmentMapper;
    private final ScheduleService scheduleService;
    private final ScheduleBlockRepository scheduleBlockRepository;
    private final AvailableDayRepository availableDayRepository;
    private final NotificationService notificationService;  // ДОБАВИТЬ

    // ========== ДЛЯ КЛИЕНТОВ ==========

    @Transactional
    public Appointment createClientAppointment(String clientPhone, CreateAppointmentRequest request) {
        try {
            // 1. Находим клиента
            Client client = clientRepository.findByUserPhone(clientPhone)
                    .orElseThrow(() -> new RuntimeException("Клиент не найден"));

            // 2. Проверяем услугу
            Service service = serviceRepository.findById(request.getServiceId())
                    .filter(Service::getIsActive)
                    .orElseThrow(() -> new RuntimeException("Услуга не найдена или неактивна"));

            // 3. Проверяем, что время соответствует правилам для клиентов
            if (!scheduleService.canBookClientSlot(request.getStartTime(), service.getDurationMinutes())) {
                throw new RuntimeException("Невозможно записаться на это время. Выберите доступный слот.");
            }

            // 4. Создаем запись (isManual = false)
            Appointment appointment = createAppointment(client, service, request.getStartTime(),
                    request.getClientNotes(), false);

            log.info("Клиент {} записался на услугу {} в {}",
                    clientPhone, service.getName(), request.getStartTime());

            // ДОБАВИТЬ: Уведомление мастеру о новой записи
            notificationService.notifyMasterNewAppointment(appointment);

            // ДОБАВИТЬ: Если запись создана менее чем за день, сразу запрос подтверждения
            if (isLessThanDayBefore(appointment.getStartTime())) {
                notificationService.notifyClientConfirmRequest(appointment);
            }

            return appointment;
        } catch (DataIntegrityViolationException e) {
            if (e.getMessage() != null && e.getMessage().contains("idx_unique_active_appointment_time")) {
                log.warn("Попытка двойной записи на время: {} от клиента: {}",
                        request.getStartTime(), clientPhone);
                throw new RuntimeException("Это время только что заняли. Пожалуйста, выберите другое время.");
            }
            throw e;
        }
    }

    // ========== ДЛЯ МАСТЕРА ==========

    @Transactional
    public Appointment createMasterAppointment(CreateMasterAppointmentRequest request) {
        // 1. Валидация запроса
        if (!request.isValid()) {
            throw new RuntimeException("Укажите либо ID существующего клиента, либо телефон и имя нового клиента");
        }

        // 2. Находим или создаем клиента
        Client client = findOrCreateClient(request);

        // 3. Проверяем услугу
        Service service = serviceRepository.findById(request.getServiceId())
                .filter(Service::getIsActive)
                .orElseThrow(() -> new RuntimeException("Услуга не найдена или неактивна"));

        // 4. Проверяем доступность времени (для мастера - более гибкие правила)
        if (!scheduleService.canBookMasterSlot(request.getStartTime(), service.getDurationMinutes())) {
            throw new RuntimeException("Это время уже занято");
        }

        // 5. Создаем запись (isManual = true)
        Appointment appointment = createAppointment(client, service, request.getStartTime(),
                request.getNotes(), true);

        log.info("Мастер создал ручную запись для {} на услугу {} в {}",
                client.getFirstName(), service.getName(), request.getStartTime());

        // ДОБАВИТЬ: Уведомление клиенту о новой записи (если нужно)
        // notificationService.notifyClientNewAppointment(appointment);

        return appointment;
    }

    // ========== ОБЩИЕ МЕТОДЫ ==========

    private Appointment createAppointment(Client client, Service service,
                                          LocalDateTime startTime, String notes, boolean isManual) {
        Appointment appointment = new Appointment();
        appointment.setClient(client);
        appointment.setService(service);
        appointment.setStartTime(startTime);
        appointment.setEndTime(calculateEndTime(startTime, service));
        appointment.setStatus(AppointmentStatus.CREATED);
        appointment.setClientNotes(notes);
        appointment.setIsManual(isManual);

        return appointmentRepository.save(appointment);
    }

    private LocalDateTime calculateEndTime(LocalDateTime startTime, Service service) {
        int duration = service.getDurationMinutes();
        int slots = (int) Math.ceil(duration / 30.0);
        return startTime.plusMinutes(slots * 30L);
    }

    private Client findOrCreateClient(CreateMasterAppointmentRequest request) {
        if (request.getClientId() != null) {
            return clientRepository.findById(request.getClientId())
                    .orElseThrow(() -> new RuntimeException("Клиент не найден"));
        }

        String phone = request.getClientPhone();
        String name = request.getClientName();
        String lastName = request.getClientLastName();

        Optional<Client> existingClient = clientRepository.findByUserPhone(phone);
        if (existingClient.isPresent()) {
            return existingClient.get();
        }

        User user = new User();
        user.setPhone(phone);
        user.setPassword("TEMPORARY_PASSWORD");
        user.setRole(Role.CLIENT);
        user.setEnabled(true);
        user = userRepository.save(user);

        Client client = new Client();
        client.setUser(user);
        client.setFirstName(name);
        client.setLastName(lastName != null ? lastName : "");
        return clientRepository.save(client);
    }

    // ========== МЕТОДЫ ЧТЕНИЯ ==========

    @Transactional(readOnly = true)
    public List<Appointment> getClientAppointments(String clientPhone) {
        Client client = clientRepository.findByUserPhone(clientPhone)
                .orElseThrow(() -> new RuntimeException("Клиент не найден"));
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        return appointmentRepository.findByClientIdAndDateAfter(client.getId(), weekAgo);
    }

    @Transactional(readOnly = true)
    public List<Appointment> getClientAppointmentsByStatus(String clientPhone, AppointmentStatus status) {
        Client client = clientRepository.findByUserPhone(clientPhone)
                .orElseThrow(() -> new RuntimeException("Клиент не найден"));
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        return appointmentRepository.findByStatusAndDateAfter(status, weekAgo).stream()
                .filter(a -> a.getClient().getId().equals(client.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Appointment> getClientAppointmentsByDate(String clientPhone, LocalDate date) {
        Client client = clientRepository.findByUserPhone(clientPhone)
                .orElseThrow(() -> new RuntimeException("Клиент не найден"));
        return appointmentRepository.findByDate(date).stream()
                .filter(a -> a.getClient().getId().equals(client.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<Appointment> getClientAppointmentById(String clientPhone, Long appointmentId) {
        Client client = clientRepository.findByUserPhone(clientPhone)
                .orElseThrow(() -> new RuntimeException("Клиент не найден"));
        return appointmentRepository.findById(appointmentId)
                .filter(a -> a.getClient().getId().equals(client.getId()));
    }

    @Transactional(readOnly = true)
    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Appointment> getAppointmentsByDate(LocalDate date) {
        return appointmentRepository.findByDate(date);
    }

    @Transactional(readOnly = true)
    public List<Appointment> getAppointmentsByStatus(AppointmentStatus status) {
        LocalDateTime monthAgo = LocalDateTime.now().minusDays(30);
        return appointmentRepository.findByStatusAndDateAfter(status, monthAgo);
    }

    @Transactional(readOnly = true)
    public List<Appointment> getAppointmentsByDateAndStatus(LocalDate date, AppointmentStatus status) {
        return appointmentRepository.findByDate(date).stream()
                .filter(a -> a.getStatus() == status)
                .toList();
    }

    // ========== МЕТОДЫ ОБНОВЛЕНИЯ ==========

    @Transactional
    public Appointment updateAppointmentStatus(Long appointmentId, UpdateAppointmentStatusRequest request) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Запись не найдена"));

        AppointmentStatus oldStatus = appointment.getStatus();
        AppointmentStatus newStatus = request.getStatus();

        appointment.setStatus(newStatus);
        appointment.setMasterNotes(request.getMasterNotes());

        if (newStatus == AppointmentStatus.COMPLETED) {
            if (request.getActualPrice() != null) {
                appointment.setActualPrice(request.getActualPrice());
            }
            if (request.getActualServices() != null && !request.getActualServices().isEmpty()) {
                appointment.setActualServices(request.getActualServices());
            }
            if (request.getMasterCompletionComment() != null) {
                appointment.setMasterCompletionComment(request.getMasterCompletionComment());
            }
        }

        if (request.getExtraMetadata() != null) {
            for (Map.Entry<String, Object> entry : request.getExtraMetadata().entrySet()) {
                appointment.putMetadata(entry.getKey(), entry.getValue());
            }
        }

        Appointment saved = appointmentRepository.save(appointment);

        // Уведомление клиенту об изменении статуса
        notificationService.notifyClientStatusChanged(saved, newStatus.name());

        // Уведомление мастеру (если клиент что-то изменил)
        if (!oldStatus.equals(newStatus)) {
            notificationService.notifyMasterStatusChanged(saved, oldStatus.name(), newStatus.name());
        }

        return saved;
    }

    @Transactional
    public Appointment rescheduleAppointment(Long appointmentId, LocalDateTime newStartTime) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Запись не найдена"));

        boolean isManual = appointment.getIsManual() != null && appointment.getIsManual();

        if (isManual) {
            if (!scheduleService.canBookMasterSlot(newStartTime,
                    appointment.getService().getDurationMinutes())) {
                throw new RuntimeException("Новое время уже занято");
            }
        } else {
            if (!scheduleService.canBookClientSlot(newStartTime,
                    appointment.getService().getDurationMinutes())) {
                throw new RuntimeException("Нельзя перенести на это время. Выберите доступный слот.");
            }
        }

        appointment.setStartTime(newStartTime);
        appointment.setEndTime(calculateEndTime(newStartTime, appointment.getService()));

        Appointment updatedAppointment = appointmentRepository.save(appointment);

        log.info("Запись ID: {} перенесена с {} на {}",
                appointmentId, appointment.getStartTime(), newStartTime);

        return updatedAppointment;
    }

    @Transactional
    public void deleteAppointment(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Запись не найдена"));
        appointmentRepository.delete(appointment);
        log.info("Запись ID: {} удалена", appointmentId);
    }

    public List<Appointment> getClientAppointmentsByStatusAndDate(String clientPhone,
                                                                  AppointmentStatus status,
                                                                  LocalDate date) {
        Client client = clientRepository.findByUserPhone(clientPhone)
                .orElseThrow(() -> new RuntimeException("Клиент не найден"));
        return appointmentRepository.findByDate(date).stream()
                .filter(a -> a.getClient().getId().equals(client.getId()))
                .filter(a -> a.getStatus() == status)
                .toList();
    }

    @Transactional
    public Appointment cancelClientAppointment(String clientPhone, Long appointmentId) {
        Appointment appointment = getClientAppointmentById(clientPhone, appointmentId)
                .orElseThrow(() -> new RuntimeException("Запись не найдена или недоступна"));

        if (appointment.getStatus() != AppointmentStatus.CREATED &&
                appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new RuntimeException("Нельзя отменить запись в статусе: " + appointment.getStatus());
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        Appointment cancelledAppointment = appointmentRepository.save(appointment);

        log.info("Клиент {} отменил запись ID: {}", clientPhone, appointmentId);

        // ДОБАВИТЬ: Уведомление мастеру об отмене
        notificationService.notifyMasterStatusChanged(cancelledAppointment, "ACTIVE", "CANCELLED");

        return cancelledAppointment;
    }

    public AppointmentResponse getAppointmentResponseById(Long id) {
        return appointmentRepository.findById(id)
                .map(appointmentMapper::toResponse)
                .orElse(null);
    }

    public List<AppointmentResponse> getClientAppointmentResponses(String clientPhone) {
        Client client = clientRepository.findByUserPhone(clientPhone)
                .orElseThrow(() -> new RuntimeException("Клиент не найден"));
        LocalDateTime monthAgo = LocalDateTime.now().minusDays(30);
        return appointmentRepository.findByClientIdAndDateAfter(client.getId(), monthAgo).stream()
                .map(appointmentMapper::toResponse)
                .toList();
    }

    @Transactional
    public Appointment moveAppointment(Long id, LocalDateTime newStartTime, Long newServiceId) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Запись не найдена"));

        Service service;
        if (newServiceId != null) {
            service = serviceRepository.findById(newServiceId)
                    .filter(Service::getIsActive)
                    .orElseThrow(() -> new RuntimeException("Услуга не найдена или неактивна"));
        } else {
            service = appointment.getService();
        }

        LocalDateTime newEndTime = calculateEndTime(newStartTime, service);

        boolean hasOverlap = appointmentRepository.existsOverlappingExcludingId(
                newStartTime, newEndTime, id);

        if (hasOverlap) {
            throw new RuntimeException("Выбранное время уже занято");
        }

        List<ScheduleBlock> blocks = scheduleBlockRepository.findBlocksInRange(
                newStartTime, newEndTime);

        if (!blocks.isEmpty()) {
            throw new RuntimeException("Это время заблокировано");
        }

        LocalDate newDate = newStartTime.toLocalDate();
        AvailableDay availableDay = availableDayRepository
                .findByAvailableDateAndIsAvailableTrue(newDate)
                .orElseThrow(() -> new RuntimeException("На эту дату нет рабочего дня"));

        if (newStartTime.toLocalTime().isBefore(availableDay.getWorkStart()) ||
                newEndTime.toLocalTime().isAfter(availableDay.getWorkEnd())) {
            throw new RuntimeException("Время выходит за пределы рабочего дня");
        }

        saveMoveHistory(appointment, newStartTime, newServiceId);

        appointment.setStartTime(newStartTime);
        appointment.setEndTime(newEndTime);
        if (newServiceId != null) {
            appointment.setService(service);
        }

        Appointment moved = appointmentRepository.save(appointment);
        log.info("Запись {} перемещена на {} (админ: {})",
                id, newStartTime, SecurityContextHolder.getContext().getAuthentication().getName());

        return moved;
    }

    private void saveMoveHistory(Appointment appointment, LocalDateTime newTime, Long newServiceId) {
        // TODO: реализовать историю изменений
    }

    // ДОБАВИТЬ ВСПОМОГАТЕЛЬНЫЙ МЕТОД
    private boolean isLessThanDayBefore(LocalDateTime startTime) {
        return startTime.minusDays(1).isBefore(LocalDateTime.now());
    }
}