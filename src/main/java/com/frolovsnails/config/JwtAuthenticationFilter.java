package com.frolovsnails.config;

import com.frolovsnails.service.JwtService;
import com.frolovsnails.service.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userPhone;

        // Пропускаем публичные эндпоинты
        String requestURI = request.getRequestURI();
        if (requestURI.contains("/swagger") ||
                requestURI.contains("/api-docs") ||
                requestURI.contains("/api/auth") ||
                requestURI.contains("/api/test") ||
                requestURI.equals("/") ||
                requestURI.equals("/api")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Проверяем наличие токена
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("JWT Token отсутствует в заголовке Authorization");
            filterChain.doFilter(request, response);
            return;
        }

        // Извлекаем токен
        jwt = authHeader.substring(7);

        try {
            userPhone = jwtService.extractUsername(jwt);

            if (userPhone != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Загружаем пользователя
                UserDetails userDetails = userDetailsService.loadUserByUsername(userPhone);

                // Проверяем валидность токена
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("Аутентифицирован пользователь: {}", userPhone);
                } else {
                    log.warn("Невалидный JWT токен для пользователя: {}", userPhone);
                }
            }
        } catch (Exception e) {
            log.error("Ошибка аутентификации по JWT: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}