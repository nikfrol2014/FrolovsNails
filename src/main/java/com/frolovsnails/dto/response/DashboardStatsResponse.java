package com.frolovsnails.dto.response.stats;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {
    // Основные метрики
    private PeriodStats currentPeriod;
    private PeriodStats previousPeriod;
    private ComparisonStats comparison;

    // Графики
    private List<DailyStats> dailyStats;
    private List<TopServiceStats> topServices;
    private List<TopClientStats> topClients;
    private List<HourlyStats> peakHours;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PeriodStats {
        private int totalAppointments;
        private int completedAppointments;
        private int cancelledAppointments;
        private int createdAppointments;
        private int confirmedAppointments;
        private int noShowAppointments; // не пришли
        private BigDecimal totalRevenue;
        private BigDecimal averageCheck;
        private int newClientsCount;
        private double occupancyRate; // загруженность в %
        private int workingDaysCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComparisonStats {
        private double appointmentsChange; // % изменения
        private double revenueChange;
        private double averageCheckChange;
        private double occupancyChange;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyStats {
        private LocalDate date;
        private int appointmentsCount;
        private BigDecimal revenue;
        private int completedCount;
        private int cancelledCount;
        private String dayOfWeek;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopServiceStats {
        private Long serviceId;
        private String serviceName;
        private String category;
        private int count;
        private BigDecimal totalRevenue;
        private BigDecimal averagePrice;
        private int durationMinutes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopClientStats {
        private Long clientId;
        private String firstName;
        private String lastName;
        private String phone;
        private int totalVisits;
        private BigDecimal totalSpent;
        private BigDecimal averageCheck;
        private LocalDate lastVisitDate;
        private String favoriteService;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HourlyStats {
        private int hour;
        private int appointmentsCount;
        private BigDecimal revenue;
        private double occupancyRate; // % загруженности в этот час
    }
}