package com.frolovsnails.repository;

import com.frolovsnails.entity.Appointment;
import com.frolovsnails.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByClientId(Long clientId);
    List<Appointment> findByClientIdAndStatus(Long clientId, AppointmentStatus status);
    List<Appointment> findByWorkSlotDateAndStatus(java.time.LocalDate date, AppointmentStatus status);
    List<Appointment> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    long countByStatus(AppointmentStatus status);
}