package com.frolovsnails.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // ВСЕ публичные эндпоинты (Swagger, статика, аутентификация)
                        .requestMatchers(
                                // Swagger UI
                                "/",
                                "/docs",
                                "/api",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/v3/api-docs",
                                "/api-docs/**",
                                "/api-docs",
                                "/swagger-resources/**",
                                "/swagger-resources",
                                "/configuration/ui",
                                "/configuration/security",
                                "/webjars/**",
                                "/webjars",
                                "/favicon.ico",

                                // Аутентификация
                                "/api/auth/**",

                                // Публичные тестовые эндпоинты
                                "/api/test/health",
                                "/api/test/db-status",
                                "/api/test/public",
                                "/api/test/create-test-services", // для создания тестовых услуг

                                // Публичные эндпоинты услуг
                                "/api/services",
                                "/api/services/**",
                                "/api/services/categories",
                                "/api/services/category/**",

                                // Публичный доступ к просмотру доступных слотов
                                "/api/schedule/availability",
                                "/api/schedule/availability/**",
                                "/api/schedule/availability/range",

                                // Статические ресурсы (если будут)
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/favicon.ico",

                                // Ошибки
                                "/error",
                                "/error/**"
                        ).permitAll()

                        // POST, PUT, DELETE для услуг требуют роли ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/services").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/services/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/services/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/services/**").hasRole("ADMIN")

                        // Управление расписанием - только для ADMIN
                        .requestMatchers(HttpMethod.GET, "/api/schedule/slots/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/schedule/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/schedule/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/schedule/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/schedule/**").hasRole("ADMIN")

                        // Все остальные требуют аутентификации
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}