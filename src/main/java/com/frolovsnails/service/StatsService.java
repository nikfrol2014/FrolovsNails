package com.frolovsnails.service;

import com.frolovsnails.dto.response.stats.DashboardStatsResponse;
import com.frolovsnails.entity.Appointment;
import com.frolovsnails.entity.AppointmentStatus;
import com.frolovsnails.entity.AvailableDay;
import com.frolovsnails.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatsService {

    private final AppointmentRepository appointmentRepository;
    private final AvailableDayRepository availableDayRepository;
    private final ClientRepository clientRepository;
    private final ServiceRepository serviceRepository;

    public DashboardStatsResponse getDashboardStats(LocalDate startDate, LocalDate endDate) {
        log.info("Getting dashboard stats for period: {} - {}", startDate, endDate);

        // Получаем все записи за период
        List<Appointment> appointments = appointmentRepository.findByDateRangeSimple(startDate, endDate);

        // Получаем рабочие дни за период для расчета загруженности
        List<AvailableDay> workingDays = availableDayRepository
                .findByAvailableDateBetweenAndIsAvailableTrue(startDate, endDate);

        // Статистика текущего периода
        DashboardStatsResponse.PeriodStats currentPeriod = calculatePeriodStats(appointments, workingDays);

        // Статистика предыдущего периода (для сравнения)
        long daysDiff = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        LocalDate prevStartDate = startDate.minusDays(daysDiff);
        LocalDate prevEndDate = endDate.minusDays(daysDiff);
        List<Appointment> prevAppointments = appointmentRepository.findByDateRangeSimple(prevStartDate, prevEndDate);
        List<AvailableDay> prevWorkingDays = availableDayRepository
                .findByAvailableDateBetweenAndIsAvailableTrue(prevStartDate, prevEndDate);
        DashboardStatsResponse.PeriodStats previousPeriod = calculatePeriodStats(prevAppointments, prevWorkingDays);

        // Сравнение
        DashboardStatsResponse.ComparisonStats comparison = calculateComparison(currentPeriod, previousPeriod);

        // Ежедневная статистика для графика
        List<DashboardStatsResponse.DailyStats> dailyStats = calculateDailyStats(appointments, startDate, endDate, workingDays);

        // Топ услуг
        List<DashboardStatsResponse.TopServiceStats> topServices = calculateTopServices(appointments, 5);

        // Топ клиентов
        List<DashboardStatsResponse.TopClientStats> topClients = calculateTopClients(appointments, 5);

        // Часы пик
        List<DashboardStatsResponse.HourlyStats> peakHours = calculatePeakHours(appointments, workingDays);

        return DashboardStatsResponse.builder()
                .currentPeriod(currentPeriod)
                .previousPeriod(previousPeriod)
                .comparison(comparison)
                .dailyStats(dailyStats)
                .topServices(topServices)
                .topClients(topClients)
                .peakHours(peakHours)
                .build();
    }

    public DashboardStatsResponse getStatsByPeriod(String period, LocalDate referenceDate) {
        LocalDate startDate;
        LocalDate endDate;

        if (referenceDate == null) {
            referenceDate = LocalDate.now();
        }

        switch (period.toLowerCase()) {
            case "day":
                startDate = referenceDate;
                endDate = referenceDate;
                break;
            case "week":
                startDate = referenceDate.minusDays(referenceDate.getDayOfWeek().getValue() - 1);
                endDate = startDate.plusDays(6);
                break;
            case "month":
                startDate = referenceDate.withDayOfMonth(1);
                endDate = referenceDate.withDayOfMonth(referenceDate.lengthOfMonth());
                break;
            default:
                throw new IllegalArgumentException("Invalid period: " + period);
        }

        return getDashboardStats(startDate, endDate);
    }

    private DashboardStatsResponse.PeriodStats calculatePeriodStats(List<Appointment> appointments, List<AvailableDay> workingDays) {
        int total = appointments.size();
        int completed = (int) appointments.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED).count();
        int cancelled = (int) appointments.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.CANCELLED).count();
        int created = (int) appointments.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.CREATED).count();
        int confirmed = (int) appointments.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.CONFIRMED).count();

        // Выручка (используем actualPrice если есть)
        BigDecimal totalRevenue = appointments.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED)
                .map(a -> {
                    BigDecimal actualPrice = a.getActualPrice();
                    return actualPrice != null ? actualPrice : a.getService().getPrice();
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Средний чек
        BigDecimal averageCheck = completed > 0
                ? totalRevenue.divide(BigDecimal.valueOf(completed), RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Новые клиенты за период
        int newClientsCount = (int) clientRepository.findAll().stream()
                .filter(c -> {
                    LocalDate createdAt = c.getCreatedAt();
                    return createdAt != null &&
                            !appointments.stream()
                                    .filter(a -> a.getClient().getId().equals(c.getId()))
                                    .findFirst()
                                    .isPresent();
                })
                .count();

        // Загруженность
        double occupancyRate = calculateOccupancyRate(appointments, workingDays);

        return DashboardStatsResponse.PeriodStats.builder()
                .totalAppointments(total)
                .completedAppointments(completed)
                .cancelledAppointments(cancelled)
                .createdAppointments(created)
                .confirmedAppointments(confirmed)
                .noShowAppointments(0) // TODO: реализовать учет неявок
                .totalRevenue(totalRevenue)
                .averageCheck(averageCheck)
                .newClientsCount(newClientsCount)
                .occupancyRate(occupancyRate)
                .workingDaysCount(workingDays.size())
                .build();
    }

    private double calculateOccupancyRate(List<Appointment> appointments, List<AvailableDay> workingDays) {
        if (workingDays.isEmpty()) return 0.0;

        // Общее количество доступных часов за период
        long totalAvailableMinutes = workingDays.stream()
                .mapToLong(day -> {
                    LocalTime start = day.getWorkStart();
                    LocalTime end = day.getWorkEnd();
                    return java.time.Duration.between(start, end).toMinutes();
                })
                .sum();

        // Занятые минуты (только завершенные и подтвержденные записи)
        long occupiedMinutes = appointments.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED ||
                        a.getStatus() == AppointmentStatus.CONFIRMED)
                .mapToLong(a -> java.time.Duration.between(a.getStartTime(), a.getEndTime()).toMinutes())
                .sum();

        if (totalAvailableMinutes == 0) return 0.0;
        return (double) occupiedMinutes / totalAvailableMinutes * 100;
    }

    private DashboardStatsResponse.ComparisonStats calculateComparison(
            DashboardStatsResponse.PeriodStats current,
            DashboardStatsResponse.PeriodStats previous) {

        return DashboardStatsResponse.ComparisonStats.builder()
                .appointmentsChange(calculatePercentageChange(
                        previous.getTotalAppointments(),
                        current.getTotalAppointments()))
                .revenueChange(calculatePercentageChange(
                        previous.getTotalRevenue(),
                        current.getTotalRevenue()))
                .averageCheckChange(calculatePercentageChange(
                        previous.getAverageCheck(),
                        current.getAverageCheck()))
                .occupancyChange(calculatePercentageChange(
                        previous.getOccupancyRate(),
                        current.getOccupancyRate()))
                .build();
    }

    private double calculatePercentageChange(Number oldValue, Number newValue) {
        if (oldValue == null || oldValue.doubleValue() == 0) {
            return newValue.doubleValue() > 0 ? 100.0 : 0.0;
        }
        return (newValue.doubleValue() - oldValue.doubleValue()) / oldValue.doubleValue() * 100;
    }

    private double calculatePercentageChange(BigDecimal oldValue, BigDecimal newValue) {
        if (oldValue == null || oldValue.compareTo(BigDecimal.ZERO) == 0) {
            return newValue != null && newValue.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        }
        return newValue.subtract(oldValue)
                .divide(oldValue, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    private List<DashboardStatsResponse.DailyStats> calculateDailyStats(
            List<Appointment> appointments,
            LocalDate startDate,
            LocalDate endDate,
            List<AvailableDay> workingDays) {

        Map<LocalDate, List<Appointment>> appointmentsByDay = appointments.stream()
                .collect(Collectors.groupingBy(a -> a.getStartTime().toLocalDate()));

        Set<LocalDate> workingDaysSet = workingDays.stream()
                .map(AvailableDay::getAvailableDate)
                .collect(Collectors.toSet());

        List<DashboardStatsResponse.DailyStats> dailyStats = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            List<Appointment> dayAppointments = appointmentsByDay.getOrDefault(date, Collections.emptyList());

            BigDecimal dayRevenue = dayAppointments.stream()
                    .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED)
                    .map(a -> {
                        BigDecimal actualPrice = a.getActualPrice();
                        return actualPrice != null ? actualPrice : a.getService().getPrice();
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            int completedCount = (int) dayAppointments.stream()
                    .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED).count();
            int cancelledCount = (int) dayAppointments.stream()
                    .filter(a -> a.getStatus() == AppointmentStatus.CANCELLED).count();

            dailyStats.add(DashboardStatsResponse.DailyStats.builder()
                    .date(date)
                    .dayOfWeek(getRussianDayOfWeek(date))
                    .appointmentsCount(dayAppointments.size())
                    .revenue(dayRevenue)
                    .completedCount(completedCount)
                    .cancelledCount(cancelledCount)
                    .build());
        }

        return dailyStats;
    }

    private List<DashboardStatsResponse.TopServiceStats> calculateTopServices(List<Appointment> appointments, int limit) {
        Map<Long, DashboardStatsResponse.TopServiceStats> statsMap = new HashMap<>();

        for (Appointment apt : appointments.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED)
                .toList()) {
            Long serviceId = apt.getService().getId();
            BigDecimal actualPrice = apt.getActualPrice();
            BigDecimal price = actualPrice != null ? actualPrice : apt.getService().getPrice();

            statsMap.compute(serviceId, (id, existing) -> {
                if (existing == null) {
                    return DashboardStatsResponse.TopServiceStats.builder()
                            .serviceId(serviceId)
                            .serviceName(apt.getService().getName())
                            .category(apt.getService().getCategory())
                            .count(1)
                            .totalRevenue(price)
                            .averagePrice(price)
                            .durationMinutes(apt.getService().getDurationMinutes())
                            .build();
                } else {
                    existing.setCount(existing.getCount() + 1);
                    existing.setTotalRevenue(existing.getTotalRevenue().add(price));
                    existing.setAveragePrice(existing.getTotalRevenue()
                            .divide(BigDecimal.valueOf(existing.getCount()), RoundingMode.HALF_UP));
                    return existing;
                }
            });
        }

        return statsMap.values().stream()
                .sorted((a, b) -> Integer.compare(b.getCount(), a.getCount()))
                .limit(limit)
                .toList();
    }

    private List<DashboardStatsResponse.TopClientStats> calculateTopClients(List<Appointment> appointments, int limit) {
        Map<Long, DashboardStatsResponse.TopClientStats> statsMap = new HashMap<>();

        for (Appointment apt : appointments) {
            Long clientId = apt.getClient().getId();
            boolean isCompleted = apt.getStatus() == AppointmentStatus.COMPLETED;
            BigDecimal price = isCompleted ?
                    (apt.getActualPrice() != null ? apt.getActualPrice() : apt.getService().getPrice()) :
                    BigDecimal.ZERO;

            statsMap.compute(clientId, (id, existing) -> {
                if (existing == null) {
                    return DashboardStatsResponse.TopClientStats.builder()
                            .clientId(clientId)
                            .firstName(apt.getClient().getFirstName())
                            .lastName(apt.getClient().getLastName())
                            .phone(apt.getClient().getUser().getPhone())
                            .totalVisits(1)
                            .totalSpent(isCompleted ? price : BigDecimal.ZERO)
                            .averageCheck(isCompleted ? price : BigDecimal.ZERO)
                            .lastVisitDate(apt.getStartTime().toLocalDate())
                            .favoriteService(apt.getService().getName())
                            .build();
                } else {
                    existing.setTotalVisits(existing.getTotalVisits() + 1);
                    if (isCompleted) {
                        existing.setTotalSpent(existing.getTotalSpent().add(price));
                        existing.setAverageCheck(existing.getTotalSpent()
                                .divide(BigDecimal.valueOf(existing.getTotalVisits()), RoundingMode.HALF_UP));
                        existing.setLastVisitDate(apt.getStartTime().toLocalDate());
                    }
                    return existing;
                }
            });
        }

        return statsMap.values().stream()
                .sorted((a, b) -> b.getTotalSpent().compareTo(a.getTotalSpent()))
                .limit(limit)
                .toList();
    }

    private List<DashboardStatsResponse.HourlyStats> calculatePeakHours(List<Appointment> appointments, List<AvailableDay> workingDays) {
        // Часы с 8 до 22
        Map<Integer, Integer> appointmentsByHour = new HashMap<>();
        Map<Integer, BigDecimal> revenueByHour = new HashMap<>();

        for (int hour = 8; hour <= 21; hour++) {
            appointmentsByHour.put(hour, 0);
            revenueByHour.put(hour, BigDecimal.ZERO);
        }

        for (Appointment apt : appointments) {
            int hour = apt.getStartTime().getHour();
            if (hour >= 8 && hour <= 21) {
                appointmentsByHour.merge(hour, 1, Integer::sum);
                if (apt.getStatus() == AppointmentStatus.COMPLETED) {
                    BigDecimal price = apt.getActualPrice() != null ?
                            apt.getActualPrice() : apt.getService().getPrice();
                    revenueByHour.merge(hour, price, BigDecimal::add);
                }
            }
        }

        // Максимальное количество слотов в час (обычно 2, т.к. слот 2.5 часа)
        int maxSlotsPerHour = 2;

        List<DashboardStatsResponse.HourlyStats> result = new ArrayList<>();
        for (int hour = 8; hour <= 21; hour++) {
            int count = appointmentsByHour.getOrDefault(hour, 0);
            double occupancyRate = (double) count / maxSlotsPerHour * 100;
            if (occupancyRate > 100) occupancyRate = 100;

            result.add(DashboardStatsResponse.HourlyStats.builder()
                    .hour(hour)
                    .appointmentsCount(count)
                    .revenue(revenueByHour.getOrDefault(hour, BigDecimal.ZERO))
                    .occupancyRate(occupancyRate)
                    .build());
        }

        return result;
    }

    private String getRussianDayOfWeek(LocalDate date) {
        switch (date.getDayOfWeek()) {
            case MONDAY: return "ПН";
            case TUESDAY: return "ВТ";
            case WEDNESDAY: return "СР";
            case THURSDAY: return "ЧТ";
            case FRIDAY: return "ПТ";
            case SATURDAY: return "СБ";
            case SUNDAY: return "ВС";
            default: return "";
        }
    }

    // Дополнительные методы для других эндпоинтов

    public Object getRevenueStats(LocalDate startDate, LocalDate endDate) {
        List<Appointment> appointments = appointmentRepository.findByDateRangeSimple(startDate, endDate);
        // TODO: реализовать детальную статистику по выручке
        return appointments.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED)
                .mapToDouble(a -> {
                    BigDecimal price = a.getActualPrice() != null ? a.getActualPrice() : a.getService().getPrice();
                    return price.doubleValue();
                })
                .sum();
    }

    public List<DashboardStatsResponse.TopClientStats> getTopClients(LocalDate startDate, LocalDate endDate, int limit) {
        List<Appointment> appointments = appointmentRepository.findByDateRangeSimple(startDate, endDate);
        return calculateTopClients(appointments, limit);
    }

    public List<DashboardStatsResponse.TopServiceStats> getTopServices(LocalDate startDate, LocalDate endDate, int limit) {
        List<Appointment> appointments = appointmentRepository.findByDateRangeSimple(startDate, endDate);
        return calculateTopServices(appointments, limit);
    }

    public List<DashboardStatsResponse.HourlyStats> getPeakHours(LocalDate startDate, LocalDate endDate) {
        List<Appointment> appointments = appointmentRepository.findByDateRangeSimple(startDate, endDate);
        List<AvailableDay> workingDays = availableDayRepository
                .findByAvailableDateBetweenAndIsAvailableTrue(startDate, endDate);
        return calculatePeakHours(appointments, workingDays);
    }
}