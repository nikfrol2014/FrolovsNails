package com.frolovsnails.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateAppointmentRequest {

    @NotNull(message = "ID услуги обязательно")
    private Long serviceId;

    @NotNull(message = "ID слота обязательно")
    private Long slotId;

    private String clientNotes;
}