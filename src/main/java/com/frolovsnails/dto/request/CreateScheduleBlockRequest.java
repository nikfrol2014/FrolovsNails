package com.frolovsnails.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
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
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm")
    private LocalDateTime startTime;

    @NotNull(message = "Время окончания обязательно")
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm")
    private LocalDateTime endTime;

    private String reason;  // VACATION, PERSONAL, SICK_LEAVE

    private String notes;
}