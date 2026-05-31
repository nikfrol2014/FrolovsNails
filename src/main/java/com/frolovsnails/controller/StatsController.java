package com.frolovsnails.controller;

import com.frolovsnails.dto.response.ApiResponse;
import com.frolovsnails.dto.response.stats.DashboardStatsResponse;
import com.frolovsnails.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/stats")
@Tag(name = "Statistics", description = "Статистика для мастера")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/dashboard")
    @Operation(summary = "Получить дашборд статистики")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> getDashboardStats(
            @RequestParam @DateTimeFormat(pattern = "dd.MM.yyyy") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "dd.MM.yyyy") LocalDate endDate) {

        DashboardStatsResponse stats = statsService.getDashboardStats(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Статистика загружена", stats));
    }

    @GetMapping("/dashboard/period")
    @Operation(summary = "Получить статистику за период (день/неделя/месяц)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> getStatsByPeriod(
            @RequestParam String period, // day, week, month
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd.MM.yyyy") LocalDate referenceDate) {

        DashboardStatsResponse stats = statsService.getStatsByPeriod(period, referenceDate);
        return ResponseEntity.ok(ApiResponse.success("Статистика загружена", stats));
    }

    @GetMapping("/revenue")
    @Operation(summary = "Получить статистику по выручке")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> getRevenueStats(
            @RequestParam @DateTimeFormat(pattern = "dd.MM.yyyy") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "dd.MM.yyyy") LocalDate endDate) {

        return ResponseEntity.ok(ApiResponse.success("Выручка", statsService.getRevenueStats(startDate, endDate)));
    }

    @GetMapping("/clients/top")
    @Operation(summary = "Топ клиентов")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> getTopClients(
            @RequestParam @DateTimeFormat(pattern = "dd.MM.yyyy") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "dd.MM.yyyy") LocalDate endDate,
            @RequestParam(defaultValue = "10") int limit) {

        return ResponseEntity.ok(ApiResponse.success("Топ клиентов", statsService.getTopClients(startDate, endDate, limit)));
    }

    @GetMapping("/services/top")
    @Operation(summary = "Топ услуг")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> getTopServices(
            @RequestParam @DateTimeFormat(pattern = "dd.MM.yyyy") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "dd.MM.yyyy") LocalDate endDate,
            @RequestParam(defaultValue = "10") int limit) {

        return ResponseEntity.ok(ApiResponse.success("Топ услуг", statsService.getTopServices(startDate, endDate, limit)));
    }

    @GetMapping("/peak-hours")
    @Operation(summary = "Часы пик")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> getPeakHours(
            @RequestParam @DateTimeFormat(pattern = "dd.MM.yyyy") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "dd.MM.yyyy") LocalDate endDate) {

        return ResponseEntity.ok(ApiResponse.success("Часы пик", statsService.getPeakHours(startDate, endDate)));
    }
}