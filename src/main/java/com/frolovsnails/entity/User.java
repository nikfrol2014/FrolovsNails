package com.frolovsnails.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.frolovsnails.dto.annotation.MoscowDateTime;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "password"})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String phone;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "created_at", nullable = false)
    @MoscowDateTime
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private Boolean enabled = true;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}