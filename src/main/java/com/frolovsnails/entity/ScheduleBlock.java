package com.frolovsnails.entity;

import com.frolovsnails.dto.annotation.MoscowDateTime;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "schedule_blocks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleBlock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "start_time", nullable = false)
    @MoscowDateTime
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    @MoscowDateTime
    private LocalDateTime endTime;

    @Column(name = "reason")
    private String reason;  // VACATION, PERSONAL, SICK_LEAVE, OTHER

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "is_blocked", nullable = false)
    private Boolean isBlocked = true;

    @PrePersist
    @PreUpdate
    protected void roundTimes() {
        // Округляем до минут (отбрасываем секунды и наносекунды)
        if (startTime != null) {
            startTime = startTime.withSecond(0).withNano(0);
        }
        if (endTime != null) {
            endTime = endTime.withSecond(0).withNano(0);
        }
    }
}