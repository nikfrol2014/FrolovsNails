package com.frolovsnails.repository;

import com.frolovsnails.entity.AvailableDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AvailableDayRepository extends JpaRepository<AvailableDay, Long> {

    Optional<AvailableDay> findByAvailableDate(LocalDate date);

    Optional<AvailableDay> findByAvailableDateAndIsAvailableTrue(LocalDate date);

    List<AvailableDay> findByAvailableDateBetween(LocalDate start, LocalDate end);

    List<AvailableDay> findByAvailableDateBetweenAndIsAvailableTrue(LocalDate start, LocalDate end);

    boolean existsByAvailableDate(LocalDate date);

    @Query("SELECT ad FROM AvailableDay ad WHERE ad.availableDate >= :startDate AND ad.isAvailable = true ORDER BY ad.availableDate")
    List<AvailableDay> findAvailableDaysFromDate(@Param("startDate") LocalDate startDate);

    @Query("SELECT ad.availableDate FROM AvailableDay ad " +
            "WHERE ad.availableDate BETWEEN :startDate AND :endDate " +
            "AND ad.isAvailable = true")
    List<LocalDate> findWorkingDatesInRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}