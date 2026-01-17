package com.frolovsnails.service;

import com.frolovsnails.dto.request.LoginRequest;
import com.frolovsnails.dto.request.RegisterRequest;
import com.frolovsnails.dto.response.AuthResponse;
import com.frolovsnails.entity.Client;
import com.frolovsnails.entity.Role;
import com.frolovsnails.entity.User;
import com.frolovsnails.repository.ClientRepository;
import com.frolovsnails.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        // Проверяем, существует ли пользователь
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("Пользователь с таким номером телефона уже существует");
        }

        // Создаем пользователя
        User user = new User();
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.CLIENT); // По умолчанию клиент
        user.setEnabled(true);

        user = userRepository.save(user);

        // Создаем клиента
        Client client = new Client();
        client.setUser(user);
        client.setFirstName(request.getFirstName());
        client.setLastName(request.getLastName());
        clientRepository.save(client);

        // Генерируем токены
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getPhone())
                .password(user.getPassword())
                .authorities("ROLE_" + user.getRole().name())
                .build();

        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .phone(user.getPhone())
                .role(user.getRole())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        // Аутентифицируем пользователя
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getPhone(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Получаем пользователя из БД
        User user = userRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // Получаем клиента для имени
        Client client = clientRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Профиль клиента не найден"));

        // Генерируем токены
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .phone(user.getPhone())
                .role(user.getRole())
                .firstName(client.getFirstName())
                .lastName(client.getLastName())
                .build();
    }

    public AuthResponse refreshToken(String refreshToken) {
        try {
            String phone = jwtService.extractUsername(refreshToken);

            if (phone != null) {
                // Загружаем пользователя из БД
                User user = userRepository.findByPhone(phone)
                        .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

                // Создаем UserDetails для проверки токена
                UserDetails userDetails = org.springframework.security.core.userdetails.User
                        .withUsername(user.getPhone())
                        .password("") // Пароль не нужен для проверки refresh токена
                        .authorities("ROLE_" + user.getRole().name())
                        .build();

                // Проверяем валидность refresh токена
                if (jwtService.isTokenValid(refreshToken, userDetails)) {
                    Client client = clientRepository.findByUserId(user.getId()).orElse(null);

                    // Генерируем новые токены
                    String newAccessToken = jwtService.generateToken(userDetails);
                    String newRefreshToken = jwtService.generateRefreshToken(userDetails);

                    return AuthResponse.builder()
                            .accessToken(newAccessToken)
                            .refreshToken(newRefreshToken)
                            .userId(user.getId())
                            .phone(user.getPhone())
                            .role(user.getRole())
                            .firstName(client != null ? client.getFirstName() : null)
                            .lastName(client != null ? client.getLastName() : null)
                            .build();
                }
            }
            throw new RuntimeException("Невалидный refresh токен");
        } catch (Exception e) {
            throw new RuntimeException("Ошибка обновления токена: " + e.getMessage());
        }
    }
}