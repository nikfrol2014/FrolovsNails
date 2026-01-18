package com.frolovsnails.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePasswordRequest {

    @NotBlank(message = "Старый пароль обязателен")
    private String oldPassword;

    @NotBlank(message = "Новый пароль обязателен")
    @Size(min = 6, message = "Новый пароль должен быть не менее 6 символов")
    private String newPassword;

    @NotBlank(message = "Подтверждение пароля обязательно")
    private String confirmPassword;

    // Можно добавить валидацию, что newPassword == confirmPassword
    // через @AssertTrue или в контроллере
}