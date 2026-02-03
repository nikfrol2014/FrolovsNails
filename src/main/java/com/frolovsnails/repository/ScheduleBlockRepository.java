package com.frolovsnails.repository;

import com.frolovsnails.entity.ScheduleBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScheduleBlockRepository extends JpaRepository<ScheduleBlock, Long> {

    @Query("SELECT b FROM ScheduleBlock b WHERE " +
            "b.startTime < :endTime AND b.endTime > :startTime " +
            "AND b.isBlocked = true")
    List<ScheduleBlock> findBlocksInRange(@Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);

    List<ScheduleBlock> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);
}