package com.frolovsnails.dto.request;

import com.frolovsnails.entity.AppointmentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAppointmentStatusRequest {

    @NotNull(message = "Статус обязателен")
    private AppointmentStatus status;

    private String masterNotes;

    // Новые поля для фактических данных при завершении
    private BigDecimal actualPrice;
    private String actualServices;
    private String masterCompletionComment;

    // Дополнительные метаданные (на будущее)
    private Map<String, Object> extraMetadata;
}