package com.frolovsnails.entity;

public enum SlotStatus {
    AVAILABLE,  // Доступен для записи
    BOOKED,     // Забронирован (есть запись)
    BLOCKED     // Заблокирован мастером (отпуск, болезнь)
}