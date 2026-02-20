package com.frolovsnails.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDate;

@Data
public class UpdateClientRequest {

    @Size(max = 100, message = "Имя не более 100 символов")
    private String firstName;

    @Size(max = 100, message = "Фамилия не более 100 символов")
    private String lastName;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Неверный формат телефона")
    private String phone;

    private LocalDate birthDate;

    @Size(max = 1000, message = "Заметки не более 1000 символов")
    private String notes;
}