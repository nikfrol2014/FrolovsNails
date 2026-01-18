package com.frolovsnails.service;

import com.frolovsnails.dto.request.CreateAppointmentRequest;
import com.frolovsnails.dto.request.UpdateAppointmentStatusRequest;
import com.frolovsnails.dto.response.AppointmentResponse;
import com.frolovsnails.entity.*;
import com.frolovsnails.mapper.AppointmentMapper;
import com.frolovsnails.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final ClientRepository clientRepository;
    private final ServiceRepository serviceRepository;
    private final WorkSlotRepository workSlotRepository;
    private final UserRepository userRepository;
    private final AppointmentMapper appointmentMapper;

    @Transactional
    public Appointment createAppointment(String clientPhone, CreateAppointmentRequest request) {
        // 1. Находим клиента
        Client client = clientRepository.findByUserPhone(clientPhone)
                .orElseThrow(() -> new RuntimeException("Клиент не найден"));

        // 2. Проверяем услугу
        Service service = serviceRepository.findById(request.getServiceId())
                .filter(Service::getIsActive)
                .orElseThrow(() -> new RuntimeException("Услуга не найдена или неактивна"));

        // 3. Проверяем слот
        WorkSlot slot = workSlotRepository.findById(request.getSlotId())
                .orElseThrow(() -> new RuntimeException("Слот не найден"));

        // 4. Проверяем доступность слота
        if (slot.getStatus() != SlotStatus.AVAILABLE) {
            throw new RuntimeException("Слот недоступен для записи. Статус: " + slot.getStatus());
        }

        // 5. Создаем запись
        Appointment appointment = new Appointment();
        appointment.setClient(client);
        appointment.setService(service);
        appointment.setWorkSlot(slot);
        appointment.setStatus(AppointmentStatus.CREATED);
        appointment.setClientNotes(request.getClientNotes());

        Appointment savedAppointment = appointmentRepository.save(appointment);

        // 6. Обновляем статус слота
        slot.setStatus(SlotStatus.BOOKED);
        workSlotRepository.save(slot);

        log.info("Создана новая запись ID: {} для клиента: {} на услугу: {}",
                savedAppointment.getId(), client.getUser().getPhone(), service.getName());

        return savedAppointment;
    }

    // ========== ОПТИМИЗИРОВАННЫЕ МЕТОДЫ ЧТЕНИЯ ==========

    @Transactional(readOnly = true)
    public List<Appointment> getClientAppointments(String clientPhone) {
        Client client = clientRepository.findByUserPhone(clientPhone)
                .orElseThrow(() -> new RuntimeException("Клиент не найден"));

        // Используем оптимизированный метод с EntityGraph
        return appointmentRepository.findByClientId(client.getId());
    }

    @Transactional(readOnly = true)
    public List<Appointment> getClientAppointmentsByStatus(String clientPhone, AppointmentStatus status) {
        Client client = clientRepository.findByUserPhone(clientPhone)
                .orElseThrow(() -> new RuntimeException("Клиент не найден"));

        return appointmentRepository.findByClientIdAndStatus(client.getId(), status);
    }

    @Transactional(readOnly = true)
    public List<Appointment> getClientAppointmentsByDate(String clientPhone, LocalDate date) {
        Client client = clientRepository.findByUserPhone(clientPhone)
                .orElseThrow(() -> new RuntimeException("Клиент не найден"));

        return appointmentRepository.findByClientIdAndDate(client.getId(), date);
    }

    @Transactional(readOnly = true)
    public Optional<Appointment> getClientAppointmentById(String clientPhone, Long appointmentId) {
        Client client = clientRepository.findByUserPhone(clientPhone)
                .orElseThrow(() -> new RuntimeException("Клиент не найден"));

        // Сначала находим запись с графом, потом проверяем принадлежность
        return appointmentRepository.findById(appointmentId)
                .filter(a -> a.getClient().getId().equals(client.getId()));
    }

    // ========== МЕТОДЫ ДЛЯ АДМИНИСТРАТОРА ==========

    @Transactional(readOnly = true)
    public List<Appointment> getAllAppointments() {
        // Используем граф для загрузки всех связей
        return appointmentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Appointment> getAppointmentsByDate(LocalDate date) {
        return appointmentRepository.findByDate(date);
    }

    @Transactional(readOnly = true)
    public List<Appointment> getAppointmentsByStatus(AppointmentStatus status) {
        return appointmentRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<Appointment> getAppointmentsByDateAndStatus(LocalDate date, AppointmentStatus status) {
        return appointmentRepository.findByDateAndStatus(date, status);
    }

    // ========== МЕТОДЫ ОБНОВЛЕНИЯ ==========

    @Transactional
    public Appointment updateAppointmentStatus(Long appointmentId, UpdateAppointmentStatusRequest request) {
        // Находим запись с графом
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Запись не найдена"));

        AppointmentStatus oldStatus = appointment.getStatus();
        AppointmentStatus newStatus = request.getStatus();

        validateStatusTransition(oldStatus, newStatus);

        appointment.setStatus(newStatus);
        appointment.setMasterNotes(request.getMasterNotes());

        // Если запись отменена или завершена, освобождаем слот
        if (newStatus == AppointmentStatus.CANCELLED || newStatus == AppointmentStatus.COMPLETED) {
            WorkSlot slot = appointment.getWorkSlot();
            slot.setStatus(SlotStatus.AVAILABLE);
            workSlotRepository.save(slot);
            log.info("Слот ID: {} освобожден после статуса: {}", slot.getId(), newStatus);
        }

        Appointment updatedAppointment = appointmentRepository.save(appointment);

        log.info("Статус записи ID: {} изменен с {} на {}",
                appointmentId, oldStatus, newStatus);

        return updatedAppointment;
    }

    @Transactional
    public Appointment rescheduleAppointment(Long appointmentId, Long newSlotId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Запись не найдена"));

        WorkSlot oldSlot = appointment.getWorkSlot();
        WorkSlot newSlot = workSlotRepository.findById(newSlotId)
                .orElseThrow(() -> new RuntimeException("Новый слот не найден"));

        if (newSlot.getStatus() != SlotStatus.AVAILABLE) {
            throw new RuntimeException("Новый слот недоступен. Статус: " + newSlot.getStatus());
        }

        // Освобождаем старый слот
        oldSlot.setStatus(SlotStatus.AVAILABLE);
        workSlotRepository.save(oldSlot);

        // Занимаем новый слот
        newSlot.setStatus(SlotStatus.BOOKED);
        workSlotRepository.save(newSlot);

        // Обновляем запись
        appointment.setWorkSlot(newSlot);
        Appointment updatedAppointment = appointmentRepository.save(appointment);

        log.info("Запись ID: {} перенесена со слота {} на слот {}",
                appointmentId, oldSlot.getId(), newSlot.getId());

        return updatedAppointment;
    }

    @Transactional
    public void deleteAppointment(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Запись не найдена"));

        // Освобождаем слот если запись не отменена
        if (appointment.getStatus() != AppointmentStatus.CANCELLED) {
            WorkSlot slot = appointment.getWorkSlot();
            slot.setStatus(SlotStatus.AVAILABLE);
            workSlotRepository.save(slot);
        }

        appointmentRepository.delete(appointment);

        log.info("Запись ID: {} удалена", appointmentId);
    }



    public List<Appointment> getClientAppointmentsByStatusAndDate(String clientPhone,
                                                                  AppointmentStatus status,
                                                                  LocalDate date) {
        Client client = clientRepository.findByUserPhone(clientPhone)
                .orElseThrow(() -> new RuntimeException("Клиент не найден"));

        return appointmentRepository.findAll().stream()
                .filter(a -> a.getClient().getId().equals(client.getId()))
                .filter(a -> a.getStatus() == status)
                .filter(a -> a.getWorkSlot().getDate().equals(date))
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

        // Освобождаем слот
        WorkSlot slot = appointment.getWorkSlot();
        slot.setStatus(SlotStatus.AVAILABLE);
        workSlotRepository.save(slot);

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

        return appointmentRepository.findByClientId(client.getId()).stream()
                .map(appointmentMapper::toResponse)
                .toList();
    }
}