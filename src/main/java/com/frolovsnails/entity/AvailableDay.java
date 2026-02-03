package com.frolovsnails.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "available_days")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailableDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "available_date", nullable = false, unique = true)
    private LocalDate availableDate;

    @Column(name = "work_start", nullable = false)
    private LocalTime workStart;

    @Column(name = "work_end", nullable = false)
    private LocalTime workEnd;

    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable = true;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}