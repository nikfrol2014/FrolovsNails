package com.frolovsnails.service;

import com.frolovsnails.entity.Appointment;
import com.frolovsnails.entity.AppointmentStatus;
import com.frolovsnails.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class NotificationScheduler {

    private final AppointmentRepository appointmentRepository;
    private final NotificationService notificationService;

    // Каждый час проверяем записи, требующие подтверждения (за день до записи)
    @Scheduled(cron = "0 0 * * * *")
    public void sendConfirmRequests() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dayFromNow = now.plusDays(1);

        List<Appointment> appointments = appointmentRepository.findByStartTimeBetweenAndStatus(
                now, dayFromNow, AppointmentStatus.CREATED);

        for (Appointment apt : appointments) {
            notificationService.notifyClientConfirmRequest(apt);
            log.info("Sent confirm request for appointment {}", apt.getId());
        }
    }

    // Каждые 30 минут отправляем напоминания за 2 часа до записи
    @Scheduled(cron = "0 */30 * * * *")
    public void sendReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime twoHoursLater = now.plusHours(2);

        List<Appointment> appointments = appointmentRepository.findByStartTimeBetweenAndStatus(
                now, twoHoursLater, AppointmentStatus.CONFIRMED);

        for (Appointment apt : appointments) {
            notificationService.notifyClientReminder(apt);
            log.info("Sent reminder for appointment {}", apt.getId());
        }
    }
}