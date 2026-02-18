package com.frolovsnails.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.frolovsnails.dto.annotation.MoscowDateTime;
import com.frolovsnails.entity.AppointmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentResponse {
    private Long id;
    private ClientInfo client;
    private ServiceInfo service;
    @MoscowDateTime
    private LocalDateTime startTime;  // ДОБАВЛЯЕМ
    @MoscowDateTime
    private LocalDateTime endTime;    // ДОБАВЛЯЕМ
    private AppointmentStatus status;
    private String clientNotes;
    private String masterNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientInfo {
        private Long id;
        private String firstName;
        private String lastName;
        private String phone;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceInfo {
        private Long id;
        private String name;
        private String description;
        private Integer durationMinutes;
        private BigDecimal price;
        private String category;
    }

    // УБИРАЕМ SlotInfo - больше не нужен
}