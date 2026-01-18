package com.frolovsnails.repository;

import com.frolovsnails.entity.Appointment;
import com.frolovsnails.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // ========== Переопределяем стандартные методы ==========

    @Override
    @EntityGraph(value = "appointment.with-client-service-slot")
    List<Appointment> findAll();

    @Override
    @EntityGraph(value = "appointment.with-all-details")
    Optional<Appointment> findById(Long id);

    // ========== Методы для клиентов ==========

    @EntityGraph(value = "appointment.for-client")
    @Query("SELECT a FROM Appointment a WHERE a.client.id = :clientId")
    List<Appointment> findByClientId(@Param("clientId") Long clientId);

    @EntityGraph(value = "appointment.for-client")
    @Query("SELECT a FROM Appointment a WHERE a.client.id = :clientId AND a.status = :status")
    List<Appointment> findByClientIdAndStatus(@Param("clientId") Long clientId,
                                              @Param("status") AppointmentStatus status);

    @EntityGraph(value = "appointment.for-client")
    @Query("SELECT a FROM Appointment a WHERE a.client.id = :clientId AND a.workSlot.date = :date")
    List<Appointment> findByClientIdAndDate(@Param("clientId") Long clientId,
                                            @Param("date") LocalDate date);

    // ========== Методы для администраторов ==========

    @EntityGraph(value = "appointment.with-all-details")
    @Query("SELECT a FROM Appointment a WHERE a.workSlot.date = :date AND a.status = :status")
    List<Appointment> findByDateAndStatus(@Param("date") LocalDate date,
                                          @Param("status") AppointmentStatus status);

    @EntityGraph(value = "appointment.with-client-service-slot")
    @Query("SELECT a FROM Appointment a WHERE a.workSlot.date = :date")
    List<Appointment> findByDate(@Param("date") LocalDate date);

    @EntityGraph(value = "appointment.with-client-service-slot")
    @Query("SELECT a FROM Appointment a WHERE a.status = :status")
    List<Appointment> findByStatus(@Param("status") AppointmentStatus status);

    // ========== Статистические запросы ==========

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.status = :status")
    long countByStatus(@Param("status") AppointmentStatus status);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.client.id = :clientId")
    long countByClientId(@Param("clientId") Long clientId);

    @Query("""
        SELECT a FROM Appointment a 
        WHERE a.createdAt BETWEEN :start AND :end
        ORDER BY a.createdAt DESC
    """)
    List<Appointment> findByCreatedAtBetween(@Param("start") java.time.LocalDateTime start,
                                             @Param("end") java.time.LocalDateTime end);
}