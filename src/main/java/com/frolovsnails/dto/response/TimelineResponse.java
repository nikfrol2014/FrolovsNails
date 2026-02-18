package com.frolovsnails.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class TimelineResponse {

    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalDays;
    private Integer totalAppointments;

    // Сгруппировано по дням
    private Map<LocalDate, List<AppointmentResponse>> appointmentsByDay;

    // Статистика по периоду
    private TimelineStats stats;

    @Data
    @Builder
    public static class TimelineStats {
        private Long confirmedCount;
        private Long pendingCount;
        private Long cancelledCount;
        private Long completedCount;
        private Long createdCount;

        private Integer workingDaysCount; // сколько дней есть расписание
        private Integer daysWithAppointments; // сколько дней есть записи
    }
}