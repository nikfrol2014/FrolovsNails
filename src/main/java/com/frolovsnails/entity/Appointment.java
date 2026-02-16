package com.frolovsnails.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.frolovsnails.util.DateTimeUtils;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@NamedEntityGraphs({
        @NamedEntityGraph(
                name = "appointment.with-client-service",
                attributeNodes = {
                        @NamedAttributeNode("client"),
                        @NamedAttributeNode("service")
                }
        )
})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    @JsonIgnoreProperties({"appointments", "user"})
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    @JsonIgnoreProperties({"appointments"})
    private Service service;

    @Column(name = "is_manual")
    private Boolean isManual = false; // false - запись через систему, true - ручная запись мастера

    @Version
    @Column(name = "version")
    private Long version;

    // ДОБАВЛЯЕМ startTime и endTime
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;  // Например: 2024-01-15T11:30:00

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;    // Вычисляем: startTime + service.duration

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status = AppointmentStatus.CREATED;

    @Column(name = "client_notes", columnDefinition = "TEXT")
    private String clientNotes;

    @Column(name = "master_notes", columnDefinition = "TEXT")
    private String masterNotes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = DateTimeUtils.truncateMillis(LocalDateTime.now());
        // endTime вычисляется в сервисе перед сохранением
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = DateTimeUtils.truncateMillis(LocalDateTime.now());
    }
}