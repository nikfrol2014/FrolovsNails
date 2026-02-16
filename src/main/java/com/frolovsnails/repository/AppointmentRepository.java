package com.frolovsnails.repository;

import com.frolovsnails.entity.Appointment;
import com.frolovsnails.entity.AppointmentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // Переопределяем с новым графом
    @Override
    @EntityGraph(value = "appointment.with-client-service")
    List<Appointment> findAll();

    @Override
    @EntityGraph(value = "appointment.with-client-service")
    Optional<Appointment> findById(Long id);

    // Методы для проверки пересечений
    @Query("SELECT COUNT(a) > 0 FROM Appointment a WHERE " +
            "a.status NOT IN (com.frolovsnails.entity.AppointmentStatus.CANCELLED) AND " +
            "a.startTime < :endTime AND a.endTime > :startTime")
    boolean existsOverlapping(@Param("startTime") LocalDateTime startTime,
                              @Param("endTime") LocalDateTime endTime);

    @Query("SELECT a FROM Appointment a WHERE " +
            "a.startTime >= :startOfDay AND a.startTime < :endOfDay " +
            "ORDER BY a.startTime")
    List<Appointment> findByDate(@Param("startOfDay") LocalDateTime startOfDay,
                                 @Param("endOfDay") LocalDateTime endOfDay);

    // Методы для клиентов
    @EntityGraph(value = "appointment.with-client-service")
    @Query("SELECT a FROM Appointment a WHERE a.client.id = :clientId " +
            "AND a.startTime >= :startDate " +
            "ORDER BY a.startTime DESC")
    List<Appointment> findByClientIdAndDateAfter(@Param("clientId") Long clientId,
                                                 @Param("startDate") LocalDateTime startDate);

    // Методы для администраторов
    @EntityGraph(value = "appointment.with-client-service")
    @Query("SELECT a FROM Appointment a WHERE DATE(a.startTime) = :date " +
            "ORDER BY a.startTime")
    List<Appointment> findByDate(@Param("date") LocalDate date);

    @EntityGraph(value = "appointment.with-client-service")
    @Query("SELECT a FROM Appointment a WHERE a.status = :status " +
            "AND a.startTime >= :startDate " +
            "ORDER BY a.startTime")
    List<Appointment> findByStatusAndDateAfter(@Param("status") AppointmentStatus status,
                                               @Param("startDate") LocalDateTime startDate);
}