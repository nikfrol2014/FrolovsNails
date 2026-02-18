package com.frolovsnails.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.frolovsnails.dto.annotation.MoscowDateTime;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateAppointmentRequest {

    @NotNull(message = "ID услуги обязательно")
    private Long serviceId;

    @NotNull(message = "Время начала обязательно")
    @MoscowDateTime
    private LocalDateTime startTime;  // вместо slotId

    private String clientNotes;
}