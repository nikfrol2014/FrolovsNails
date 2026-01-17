package com.frolovsnails.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Номер телефона обязателен")
    private String phone;

    @NotBlank(message = "Пароль обязателен")
    private String password;
}