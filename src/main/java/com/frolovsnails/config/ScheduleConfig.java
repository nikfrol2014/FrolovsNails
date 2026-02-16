package com.frolovsnails.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.schedule")
@Data
public class ScheduleConfig {
    private int clientSlotMinutes = 150; // значение по умолчанию
}