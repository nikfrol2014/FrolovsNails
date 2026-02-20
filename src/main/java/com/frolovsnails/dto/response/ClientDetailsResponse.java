package com.frolovsnails.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ClientDetailsResponse {

    private ClientInfo client;
    private ClientStats stats;
    private List<AppointmentResponse> recentAppointments;
    private List<AppointmentResponse> upcomingAppointments;

    @Data
    @Builder
    public static class ClientInfo {
        private Long id;
        private String firstName;
        private String lastName;
        private String phone;
        private String email;
        private LocalDate birthDate;
        private String notes;
        private LocalDate registeredAt;
        private Boolean isVip;
    }

    @Data
    @Builder
    public static class ClientStats {
        private Integer totalVisits;              // Всего визитов
        private Integer cancelledVisits;           // Отменено
        private Integer noShowVisits;               // Не пришел
        private BigDecimal totalSpent;              // Всего потрачено
        private BigDecimal averageBill;             // Средний чек
        private LocalDate firstVisitDate;           // Первый визит
        private LocalDate lastVisitDate;            // Последний визит
        private String favoriteService;             // Любимая услуга
        private Integer favoriteServiceCount;       // Сколько раз
        private String favoriteMaster;              // Любимый мастер (на будущее)
        private Double attendanceRate;               // % посещаемости
    }
}