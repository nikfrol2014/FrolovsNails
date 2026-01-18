package com.frolovsnails.dto.request;

import com.frolovsnails.entity.SlotStatus;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateWorkSlotRequest {

    @NotNull(message = "Дата обязательна")
    @FutureOrPresent(message = "Дата не может быть в прошлом")
    private LocalDate date;

    @NotNull(message = "Время начала обязательно")
    private LocalTime startTime;

    @NotNull(message = "Время окончания обязательно")
    private LocalTime endTime;

    private SlotStatus status;

    private String masterNotes;

    // Можно добавить валидацию, что endTime > startTime
    // через @AssertTrue или в контроллере
}