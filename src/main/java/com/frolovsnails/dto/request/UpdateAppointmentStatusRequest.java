package com.frolovsnails.dto.request;

import com.frolovsnails.entity.AppointmentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAppointmentStatusRequest {

    @NotNull(message = "Статус обязателен")
    private AppointmentStatus status;

    private String masterNotes;
}