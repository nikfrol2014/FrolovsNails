package com.frolovsnails.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.frolovsnails.entity.AppointmentStatus;
import com.frolovsnails.entity.Client;
import com.frolovsnails.entity.Service;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "appointments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Appointment {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

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

    @Column(name = "is_manual")
    private Boolean isManual = false;

    @Version
    @Column(name = "version")
    private Long version;

    // Новое поле для гибких метаданных
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (metadata == null) {
            metadata = new HashMap<>();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Удобные методы для работы с metadata
    public void putMetadata(String key, Object value) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getMetadata(String key, Class<T> type) {
        if (metadata == null || !metadata.containsKey(key)) {
            return null;
        }
        Object value = metadata.get(key);
        if (value == null) {
            return null;
        }
        if (type.isAssignableFrom(value.getClass())) {
            return (T) value;
        }
        // Конвертация для чисел и других типов
        if (type == BigDecimal.class && value instanceof Number) {
            return (T) BigDecimal.valueOf(((Number) value).doubleValue());
        }
        return objectMapper.convertValue(value, type);
    }

    public String getMetadataAsJson() {
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    public void setMetadataFromJson(String json) {
        try {
            this.metadata = objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            this.metadata = new HashMap<>();
        }
    }

    // Вспомогательные методы для часто используемых метаданных
    public BigDecimal getActualPrice() {
        return getMetadata("actualPrice", BigDecimal.class);
    }

    public void setActualPrice(BigDecimal actualPrice) {
        putMetadata("actualPrice", actualPrice);
    }

    public String getActualServices() {
        return getMetadata("actualServices", String.class);
    }

    public void setActualServices(String actualServices) {
        putMetadata("actualServices", actualServices);
    }

    public String getMasterCompletionComment() {
        return getMetadata("masterCompletionComment", String.class);
    }

    public void setMasterCompletionComment(String comment) {
        putMetadata("masterCompletionComment", comment);
    }
}