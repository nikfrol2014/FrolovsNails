package com.frolovsnails.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @Size(max = 100, message = "Имя должно быть не более 100 символов")
    private String firstName;

    @Size(max = 100, message = "Фамилия должна быть не более 100 символов")
    private String lastName;

    private LocalDate birthDate;

    @Size(max = 1000, message = "Заметки должны быть не более 1000 символов")
    private String notes;
}