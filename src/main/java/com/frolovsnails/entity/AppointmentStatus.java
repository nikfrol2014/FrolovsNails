package com.frolovsnails.entity;

public enum AppointmentStatus {
    CREATED,    // Создана клиентом
    PENDING,    // Ожидает подтверждения мастера
    CONFIRMED,  // Подтверждена мастером
    CANCELLED,  // Отменена
    COMPLETED   // Выполнена
}