package com.frolovsnails.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

@Configuration
public class TimeZoneConfig {

    @PostConstruct
    public void init() {
        // Устанавливаем часовой пояс для всего приложения
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Moscow"));
    }
}