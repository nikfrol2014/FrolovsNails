package com.frolovsnails.service;

import com.frolovsnails.entity.Appointment;
import com.frolovsnails.entity.Client;
import com.frolovsnails.entity.Role;
import com.frolovsnails.entity.User;
import com.frolovsnails.repository.UserRepository;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final UserRepository userRepository;
    private FirebaseMessaging firebaseMessaging;

    @PostConstruct
    public void initialize() {
        try {
            FileInputStream serviceAccount = new FileInputStream("firebase-service-account.json");
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }
            firebaseMessaging = FirebaseMessaging.getInstance();
            log.info("Firebase initialized successfully");
        } catch (IOException e) {
            log.error("Failed to initialize Firebase: {}", e.getMessage());
        }
    }

    // Уведомление мастеру о новой записи
    public void notifyMasterNewAppointment(Appointment appointment) {
        User master = userRepository.findByRole(Role.ADMIN).orElse(null);

        if (master != null && master.getFcmToken() != null) {
            String title = "🆕 Новая запись!";
            String body = String.format("%s %s записался на %s в %s",
                    appointment.getClient().getFirstName(),
                    appointment.getClient().getLastName() != null ? appointment.getClient().getLastName() : "",
                    appointment.getService().getName(),
                    appointment.getStartTime().format(DateTimeFormatter.ofPattern("dd.MM HH:mm")));

            sendNotification(master.getFcmToken(), title, body, "NEW_APPOINTMENT", appointment.getId());
        }
    }

    // Уведомление мастеру об изменении статуса
    public void notifyMasterStatusChanged(Appointment appointment, String oldStatus, String newStatus) {
        User master = userRepository.findByRole(Role.ADMIN).orElse(null);

        if (master != null && master.getFcmToken() != null) {
            String title = "📝 Статус записи изменен";
            String body = String.format("Запись от %s: статус изменен с %s на %s",
                    appointment.getClient().getFirstName(),
                    oldStatus, newStatus);

            sendNotification(master.getFcmToken(), title, body, "STATUS_CHANGED", appointment.getId());
        }
    }

    // Уведомление клиенту о необходимости подтверждения
    public void notifyClientConfirmRequest(Appointment appointment) {
        Client client = appointment.getClient();
        User user = client.getUser();

        if (user != null && user.getFcmToken() != null) {
            String title = "✏️ Подтвердите запись";
            String body = String.format("У вас запись на %s %s. Пожалуйста, подтвердите.",
                    appointment.getStartTime().format(DateTimeFormatter.ofPattern("dd.MM HH:mm")),
                    appointment.getService().getName());

            sendNotification(user.getFcmToken(), title, body, "CONFIRM_REQUEST", appointment.getId());
        }
    }

    // Уведомление клиенту об изменении статуса мастером
    public void notifyClientStatusChanged(Appointment appointment, String newStatus) {
        Client client = appointment.getClient();
        User user = client.getUser();

        if (user != null && user.getFcmToken() != null) {
            String title = getStatusTitle(newStatus);
            String body = getStatusBody(appointment, newStatus);

            sendNotification(user.getFcmToken(), title, body, "STATUS_CHANGED", appointment.getId());
        }
    }

    // Уведомление-напоминание
    public void notifyClientReminder(Appointment appointment) {
        Client client = appointment.getClient();
        User user = client.getUser();

        if (user != null && user.getFcmToken() != null) {
            String title = "⏰ Напоминание о записи";
            String body = String.format("У вас запись через 2 часа: %s в %s",
                    appointment.getService().getName(),
                    appointment.getStartTime().format(DateTimeFormatter.ofPattern("dd.MM HH:mm")));

            sendNotification(user.getFcmToken(), title, body, "REMINDER", appointment.getId());
        }
    }

    private void sendNotification(String token, String title, String body, String type, Long appointmentId) {
        if (token == null) return;

        Message message = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putData("type", type)
                .putData("appointmentId", String.valueOf(appointmentId))
                .build();

        try {
            String response = firebaseMessaging.send(message);
            log.info("Notification sent: {}", response);
        } catch (Exception e) {
            log.error("Failed to send notification: {}", e.getMessage());
        }
    }

    private String getStatusTitle(String status) {
        switch (status) {
            case "CONFIRMED": return "✅ Запись подтверждена!";
            case "CANCELLED": return "❌ Запись отменена";
            case "COMPLETED": return "✔️ Запись выполнена";
            default: return "📝 Статус записи изменен";
        }
    }

    private String getStatusBody(Appointment appointment, String status) {
        switch (status) {
            case "CONFIRMED":
                return String.format("Мастер подтвердил вашу запись на %s в %s",
                        appointment.getStartTime().format(DateTimeFormatter.ofPattern("dd.MM HH:mm")),
                        appointment.getService().getName());
            case "CANCELLED":
                return String.format("Запись на %s отменена",
                        appointment.getStartTime().format(DateTimeFormatter.ofPattern("dd.MM HH:mm")));
            case "COMPLETED":
                return "Спасибо за визит! Ждем вас снова.";
            default:
                return String.format("Статус записи на %s изменен на %s",
                        appointment.getStartTime().format(DateTimeFormatter.ofPattern("dd.MM HH:mm")),
                        status);
        }
    }
}