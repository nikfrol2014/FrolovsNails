package com.frolovsnails.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateMasterAppointmentRequest {

    @NotNull(message = "ID услуги обязательно")
    private Long serviceId;

    @NotNull(message = "Время начала обязательно")
    private LocalDateTime startTime;

    // Вариант 1: Использовать существующего клиента
    private Long clientId;

    // Вариант 2: Создать нового клиента
    private String clientPhone;
    private String clientName;
    private String clientLastName;

    private String notes;

    // Валидация: должен быть указан либо clientId, либо clientPhone+clientName
    public boolean isValid() {
        if (clientId != null) {
            return true; // Используем существующего клиента
        }
        // Создаем нового клиента
        return clientPhone != null && !clientPhone.isBlank()
                && clientName != null && !clientName.isBlank();
    }
}