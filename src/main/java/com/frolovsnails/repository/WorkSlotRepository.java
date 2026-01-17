package com.frolovsnails.repository;

import com.frolovsnails.entity.SlotStatus;
import com.frolovsnails.entity.WorkSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface WorkSlotRepository extends JpaRepository<WorkSlot, Long> {
    List<WorkSlot> findByDate(LocalDate date);
    List<WorkSlot> findByDateAndStatus(LocalDate date, SlotStatus status);
    List<WorkSlot> findByDateBetweenAndStatus(LocalDate startDate, LocalDate endDate, SlotStatus status);
    boolean existsByDateAndStartTimeAndEndTime(LocalDate date, LocalTime startTime, LocalTime endTime);
}