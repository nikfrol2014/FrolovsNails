package com.frolovsnails.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
                name = "appointment.with-client-service-slot",
                attributeNodes = {
                        @NamedAttributeNode("client"),
                        @NamedAttributeNode("service"),
                        @NamedAttributeNode("workSlot")
                }
        ),
        @NamedEntityGraph(
                name = "appointment.with-all-details",
                attributeNodes = {
                        @NamedAttributeNode("client"),
                        @NamedAttributeNode("service"),
                        @NamedAttributeNode("workSlot")
                },
                subgraphs = {
                        @NamedSubgraph(
                                name = "client.user",
                                attributeNodes = @NamedAttributeNode(value = "user")
                        )
                }
        ),
        @NamedEntityGraph(
                name = "appointment.for-client",
                attributeNodes = {
                        @NamedAttributeNode("service"),
                        @NamedAttributeNode("workSlot")
                }
        )
})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)  // ← LAZY!
    @JoinColumn(name = "client_id", nullable = false)
    @JsonIgnoreProperties({"appointments", "user"})
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)  // ← LAZY!
    @JoinColumn(name = "service_id", nullable = false)
    @JsonIgnoreProperties({"appointments"})
    private Service service;

    @ManyToOne(fetch = FetchType.LAZY)  // ← LAZY!
    @JoinColumn(name = "slot_id", nullable = false)
    @JsonIgnoreProperties({"appointments"})
    private WorkSlot workSlot;

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
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}