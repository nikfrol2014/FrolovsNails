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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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

            return appointment;
        } catch (DataIntegrityViolationException e) {
            // Проверяем, что это именно наш индекс
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
        // Округляем длительность до 30 минут вверх
        int duration = service.getDurationMinutes();
        int slots = (int) Math.ceil(duration / 30.0);
        return startTime.plusMinutes(slots * 30L);
    }

    private Client findOrCreateClient(CreateMasterAppointmentRequest request) {
        // Вариант 1: Используем существующего клиента
        if (request.getClientId() != null) {
            return clientRepository.findById(request.getClientId())
                    .orElseThrow(() -> new RuntimeException("Клиент не найден"));
        }

        // Вариант 2: Создаем нового клиента
        String phone = request.getClientPhone();
        String name = request.getClientName();
        String lastName = request.getClientLastName();

        // Проверяем, нет ли уже клиента с таким телефоном
        Optional<Client> existingClient = clientRepository.findByUserPhone(phone);
        if (existingClient.isPresent()) {
            return existingClient.get();
        }

        // Создаем нового пользователя
        User user = new User();
        user.setPhone(phone);
        user.setPassword("TEMPORARY_PASSWORD");
        user.setRole(Role.CLIENT);
        user.setEnabled(true);
        user = userRepository.save(user);

        // Создаем клиента
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

        validateStatusTransition(oldStatus, newStatus);
        appointment.setStatus(newStatus);
        appointment.setMasterNotes(request.getMasterNotes());

        Appointment updatedAppointment = appointmentRepository.save(appointment);

        log.info("Статус записи ID: {} изменен с {} на {}",
                appointmentId, oldStatus, newStatus);

        return updatedAppointment;
    }

    @Transactional
    public Appointment rescheduleAppointment(Long appointmentId, LocalDateTime newStartTime) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Запись не найдена"));

        // Проверяем доступность нового времени
        // Для ручных записей мастера - более гибкие правила
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

        // Обновляем время
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

        // Клиент может отменять только свои записи в статусах CREATED или PENDING
        if (appointment.getStatus() != AppointmentStatus.CREATED &&
                appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new RuntimeException("Нельзя отменить запись в статусе: " + appointment.getStatus());
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        Appointment cancelledAppointment = appointmentRepository.save(appointment);

        log.info("Клиент {} отменил запись ID: {}", clientPhone, appointmentId);

        return cancelledAppointment;
    }

    private void validateStatusTransition(AppointmentStatus oldStatus, AppointmentStatus newStatus) {
        // Определяем допустимые переходы статусов
        switch (oldStatus) {
            case CREATED -> {
                if (newStatus != AppointmentStatus.PENDING &&
                        newStatus != AppointmentStatus.CANCELLED) {
                    throw new RuntimeException("Неверный переход статуса: " + oldStatus + " -> " + newStatus);
                }
            }
            case PENDING -> {
                if (newStatus != AppointmentStatus.CONFIRMED &&
                        newStatus != AppointmentStatus.CANCELLED) {
                    throw new RuntimeException("Неверный переход статуса: " + oldStatus + " -> " + newStatus);
                }
            }
            case CONFIRMED -> {
                if (newStatus != AppointmentStatus.COMPLETED &&
                        newStatus != AppointmentStatus.CANCELLED) {
                    throw new RuntimeException("Неверный переход статуса: " + oldStatus + " -> " + newStatus);
                }
            }
            case CANCELLED, COMPLETED ->
                    throw new RuntimeException("Запись в статусе " + oldStatus + " не может быть изменена");
        }
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
        // 1. Находим запись
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Запись не найдена"));

        // 2. Определяем услугу (старую или новую)
        Service service;
        if (newServiceId != null) {
            service = serviceRepository.findById(newServiceId)
                    .filter(Service::getIsActive)
                    .orElseThrow(() -> new RuntimeException("Услуга не найдена или неактивна"));
        } else {
            service = appointment.getService();
        }

        // 3. Проверяем доступность нового времени
        LocalDateTime newEndTime = calculateEndTime(newStartTime, service);

        // Проверяем пересечения (исключая текущую запись)
        boolean hasOverlap = appointmentRepository.existsOverlappingExcludingId(
                newStartTime, newEndTime, id);

        if (hasOverlap) {
            throw new RuntimeException("Выбранное время уже занято");
        }

        // 4. Проверяем блокировки
        List<ScheduleBlock> blocks = scheduleBlockRepository.findBlocksInRange(
                newStartTime, newEndTime);

        if (!blocks.isEmpty()) {
            throw new RuntimeException("Это время заблокировано");
        }

        // 5. Проверяем, что время попадает в рабочий день
        LocalDate newDate = newStartTime.toLocalDate();
        AvailableDay availableDay = availableDayRepository
                .findByAvailableDateAndIsAvailableTrue(newDate)
                .orElseThrow(() -> new RuntimeException("На эту дату нет рабочего дня"));

        if (newStartTime.toLocalTime().isBefore(availableDay.getWorkStart()) ||
                newEndTime.toLocalTime().isAfter(availableDay.getWorkEnd())) {
            throw new RuntimeException("Время выходит за пределы рабочего дня");
        }

        // 6. Сохраняем историю (опционально)
        saveMoveHistory(appointment, newStartTime, newServiceId);

        // 7. Обновляем запись
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
}