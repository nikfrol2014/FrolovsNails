package com.frolovsnails.exception;

import com.frolovsnails.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.format.DateTimeParseException;

@RestControllerAdvice
public class DateTimeExceptionHandler {

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, DateTimeParseException.class})
    public ResponseEntity<ApiResponse> handleDateTimeExceptions(Exception ex) {
        String message = """
                Неверный формат даты. Используйте:\s
                • для даты: dd.MM.yyyy (например, 18.02.2026)
                • для времени: HH:mm (например, 14:30)
                • для даты и времени: dd.MM.yyyy HH:mm (например, 18.02.2026 14:30)""";

        return ResponseEntity.badRequest().body(ApiResponse.error(message));
    }
}