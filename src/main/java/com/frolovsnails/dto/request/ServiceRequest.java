package com.frolovsnails.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRequest {

    @NotBlank(message = "Название услуги обязательно")
    @Size(max = 200, message = "Название не должно превышать 200 символов")
    private String name;

    @Size(max = 1000, message = "Описание не должно превышать 1000 символов")
    private String description;

    @NotNull(message = "Длительность обязательна")
    @Min(value = 15, message = "Минимальная длительность 15 минут")
    @Max(value = 480, message = "Максимальная длительность 480 минут")
    private Integer durationMinutes;

    @NotNull(message = "Цена обязательна")
    @DecimalMin(value = "0.0", inclusive = false, message = "Цена должна быть больше 0")
    private BigDecimal price;

    @NotBlank(message = "Категория обязательна")
    @Size(max = 100, message = "Категория не должна превышать 100 символов")
    private String category;
}