package com.frolovsnails.util;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class DateTimeUtils {

    private static final ZoneId MOSCOW_ZONE = ZoneId.of("Europe/Moscow");

    // Форматтер без миллисекунд
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    // Для API ответов (без миллисекунд)
    private static final DateTimeFormatter ISO_FORMATTER =
            DateTimeFormatter.ofPattern("dd-MM-yyyy'T'HH:mm:ss");

    /**
     * Получить текущее время в Москве
     */
    public static LocalDateTime now() {
        return LocalDateTime.now(MOSCOW_ZONE);
    }

    /**
     * Конвертировать LocalDateTime в строку без миллисекунд
     */
    public static String format(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return dateTime.format(ISO_FORMATTER);
    }

    /**
     * Обрезать миллисекунды у LocalDateTime
     */
    public static LocalDateTime truncateMillis(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return dateTime.withNano(0).withSecond(0); // убираем наносекунды и секунды
    }
}