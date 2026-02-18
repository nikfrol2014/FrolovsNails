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
public class CreateScheduleBlockRequest {

    @NotNull(message = "Время начала обязательно")
    @MoscowDateTime
    private LocalDateTime startTime;

    @NotNull(message = "Время окончания обязательно")
    @MoscowDateTime
    private LocalDateTime endTime;

    private String reason;  // VACATION, PERSONAL, SICK_LEAVE

    private String notes;
}