package com.frolovsnails.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordUtil {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public String encode(String password) {
        return encoder.encode(password);
    }

    public static void main(String[] args) {
        PasswordUtil util = new PasswordUtil();
        System.out.println("Хэш пароля 'password123': " + util.encode("admin123"));
    }
}