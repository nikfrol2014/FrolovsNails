package com.frolovsnails.repository;

import com.frolovsnails.entity.SlotStatus;
import com.frolovsnails.entity.WorkSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface WorkSlotRepository extends JpaRepository<WorkSlot, Long> {

    // Простые запросы - EntityGraph не нужен
    List<WorkSlot> findByDate(LocalDate date);

    List<WorkSlot> findByDateAndStatus(LocalDate date, SlotStatus status);

    @Query("""
        SELECT ws FROM WorkSlot ws 
        WHERE ws.date BETWEEN :startDate AND :endDate 
        AND ws.status = :status
        ORDER BY ws.date, ws.startTime
    """)
    List<WorkSlot> findByDateBetweenAndStatus(@Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate,
                                              @Param("status") SlotStatus status);

    @Query("""
        SELECT CASE WHEN COUNT(ws) > 0 THEN true ELSE false END 
        FROM WorkSlot ws 
        WHERE ws.date = :date 
        AND ws.startTime = :startTime 
        AND ws.endTime = :endTime
    """)
    boolean existsByDateAndStartTimeAndEndTime(@Param("date") LocalDate date,
                                               @Param("startTime") LocalTime startTime,
                                               @Param("endTime") LocalTime endTime);
}