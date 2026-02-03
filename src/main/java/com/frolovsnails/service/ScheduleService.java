package com.frolovsnails.service;

import com.frolovsnails.entity.Appointment;
import com.frolovsnails.entity.AvailableDay;
import com.frolovsnails.entity.ScheduleBlock;
import com.frolovsnails.repository.AvailableDayRepository;
import com.frolovsnails.repository.AppointmentRepository;
import com.frolovsnails.repository.ScheduleBlockRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final AvailableDayRepository availableDayRepository;
    private final ScheduleBlockRepository scheduleBlockRepository;
    private final AppointmentRepository appointmentRepository;

    // ========== ПУБЛИЧНЫЕ МЕТОДЫ (для клиентов) ==========

    /**
     * Получить доступные слоты для клиентов на конкретную дату
     */
    public List<LocalDateTime> getAvailableSlotsForClients(LocalDate date, Integer serviceDuration) {
        // 1. Проверяем, доступен ли этот день
        AvailableDay availableDay = availableDayRepository
                .findByAvailableDateAndIsAvailableTrue(date)
                .orElseThrow(() -> new RuntimeException("На эту дату нет доступного времени для записи"));

        // 2. Получаем записи и блокировки на эту дату
        List<Appointment> appointments = getAppointmentsForDate(date);
        List<ScheduleBlock> blocks = getBlocksForDate(date);

        // 3. Генерируем слоты с шагом 2.5 часа от workStart
        LocalDateTime currentSlot = LocalDateTime.of(date, availableDay.getWorkStart());
        LocalDateTime workEnd = LocalDateTime.of(date, availableDay.getWorkEnd());

        List<LocalDateTime> availableSlots = new ArrayList<>();

        while (currentSlot.isBefore(workEnd)) {
            // Проверяем, что слот не выходит за рабочий день с учетом длительности услуги
            LocalDateTime slotEnd = currentSlot.plusMinutes(serviceDuration);
            if (slotEnd.isAfter(workEnd)) {
                break; // Услуга не помещается в оставшееся время
            }

            // Проверяем доступность слота
            if (isTimeAvailable(currentSlot, serviceDuration, appointments, blocks)) {
                availableSlots.add(currentSlot);
            }

            // Переходим к следующему слоту (+2.5 часа)
            currentSlot = currentSlot.plusMinutes(150); // 2.5 часа = 150 минут
        }

        return availableSlots;
    }

    /**
     * Проверить, можно ли записать клиента в указанный слот
     */
    public boolean canBookClientSlot(LocalDateTime startTime, Integer serviceDuration) {
        LocalDate date = startTime.toLocalDate();

        // 1. Проверяем доступность дня
        AvailableDay availableDay = availableDayRepository
                .findByAvailableDateAndIsAvailableTrue(date)
                .orElse(null);

        if (availableDay == null) {
            return false;
        }

        // 2. Проверяем, что время соответствует шагу 2.5 часа от начала рабочего дня
        if (!isOnClientSlotBoundary(startTime, availableDay.getWorkStart())) {
            return false;
        }

        // 3. Проверяем, что услуга помещается в рабочий день
        LocalDateTime endTime = startTime.plusMinutes(serviceDuration);
        if (endTime.toLocalTime().isAfter(availableDay.getWorkEnd())) {
            return false;
        }

        // 4. Получаем записи и блокировки
        List<Appointment> appointments = getAppointmentsForDate(date);
        List<ScheduleBlock> blocks = getBlocksForDate(date);

        // 5. Проверяем доступность
        return isTimeAvailable(startTime, serviceDuration, appointments, blocks);
    }

    /**
     * Проверить, можно ли записать мастера в любое время
     */
    public boolean canBookMasterSlot(LocalDateTime startTime, Integer serviceDuration) {
        LocalDate date = startTime.toLocalDate();

        // 1. Проверяем доступность дня
        AvailableDay availableDay = availableDayRepository
                .findByAvailableDateAndIsAvailableTrue(date)
                .orElse(null);

        if (availableDay == null) {
            return false;
        }

        // 2. Проверяем, что услуга помещается в рабочий день
        LocalDateTime endTime = startTime.plusMinutes(serviceDuration);
        if (endTime.toLocalTime().isAfter(availableDay.getWorkEnd())) {
            return false;
        }

        // 3. Получаем записи и блокировки
        List<Appointment> appointments = getAppointmentsForDate(date);
        List<ScheduleBlock> blocks = getBlocksForDate(date);

        // 4. Для мастера проверяем только базовую доступность
        return isTimeAvailable(startTime, serviceDuration, appointments, blocks);
    }

    /**
     * Получить все свободные промежутки времени для мастера
     */
    public List<TimeRange> getAvailableTimeRangesForMaster(LocalDate date, Integer minDuration) {
        AvailableDay availableDay = availableDayRepository
                .findByAvailableDateAndIsAvailableTrue(date)
                .orElseThrow(() -> new RuntimeException("На эту дату нет доступного времени"));

        List<Appointment> appointments = getAppointmentsForDate(date);
        List<ScheduleBlock> blocks = getBlocksForDate(date);

        LocalDateTime dayStart = LocalDateTime.of(date, availableDay.getWorkStart());
        LocalDateTime dayEnd = LocalDateTime.of(date, availableDay.getWorkEnd());

        // Находим все свободные промежутки
        return findAvailableTimeRanges(dayStart, dayEnd, appointments, blocks, minDuration);
    }

    // ========== МЕТОДЫ УПРАВЛЕНИЯ РАСПИСАНИЕМ ==========

    /**
     * Добавить доступный день
     */
    @Transactional
    public AvailableDay addAvailableDay(LocalDate date, LocalTime workStart, LocalTime workEnd, String notes) {
        // Проверяем, не существует ли уже день с этой датой
        if (availableDayRepository.existsByAvailableDate(date)) {
            throw new RuntimeException("День с датой " + date + " уже существует");
        }

        AvailableDay availableDay = new AvailableDay();
        availableDay.setAvailableDate(date);
        availableDay.setWorkStart(workStart);
        availableDay.setWorkEnd(workEnd);
        availableDay.setIsAvailable(true);
        availableDay.setNotes(notes);

        return availableDayRepository.save(availableDay);
    }

    /**
     * Обновить доступный день
     */
    @Transactional
    public AvailableDay updateAvailableDay(Long id, LocalTime workStart, LocalTime workEnd, Boolean isAvailable, String notes) {
        AvailableDay availableDay = availableDayRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("День не найден"));

        availableDay.setWorkStart(workStart);
        availableDay.setWorkEnd(workEnd);
        availableDay.setIsAvailable(isAvailable);
        availableDay.setNotes(notes);

        return availableDayRepository.save(availableDay);
    }

    /**
     * Получить доступные дни в диапазоне
     */
    public List<AvailableDay> getAvailableDays(LocalDate startDate, LocalDate endDate) {
        return availableDayRepository.findByAvailableDateBetween(startDate, endDate);
    }

    /**
     * Получить ближайшие доступные дни (для клиентов)
     */
    public List<AvailableDay> getUpcomingAvailableDays(int daysCount) {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(daysCount);

        return availableDayRepository.findByAvailableDateBetweenAndIsAvailableTrue(startDate, endDate);
    }

    /**
     * Удалить доступный день
     */
    @Transactional
    public void deleteAvailableDay(Long id) {
        AvailableDay availableDay = availableDayRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("День не найден"));

        // Проверяем, нет ли записей на этот день
        List<Appointment> appointments = getAppointmentsForDate(availableDay.getAvailableDate());
        if (!appointments.isEmpty()) {
            throw new RuntimeException("Нельзя удалить день, на который есть записи");
        }

        availableDayRepository.delete(availableDay);
    }

    /**
     * Заблокировать время (отпуск, больничный и т.д.)
     */
    @Transactional
    public ScheduleBlock blockTime(LocalDateTime startTime, LocalDateTime endTime, String reason, String notes) {
        ScheduleBlock block = new ScheduleBlock();
        block.setStartTime(startTime);
        block.setEndTime(endTime);
        block.setReason(reason);
        block.setNotes(notes);
        block.setIsBlocked(true);

        return scheduleBlockRepository.save(block);
    }

    /**
     * Разблокировать время
     */
    @Transactional
    public void unblockTime(Long blockId) {
        ScheduleBlock block = scheduleBlockRepository.findById(blockId)
                .orElseThrow(() -> new RuntimeException("Блокировка не найден"));

        block.setIsBlocked(false);
        scheduleBlockRepository.save(block);
    }

    // ========== ПРИВАТНЫЕ МЕТОДЫ ==========

    private boolean isTimeAvailable(LocalDateTime startTime, Integer serviceDuration,
                                    List<Appointment> appointments, List<ScheduleBlock> blocks) {
        LocalDateTime endTime = startTime.plusMinutes(serviceDuration);

        // 1. Проверяем блокировки
        if (isTimeBlocked(startTime, endTime, blocks)) {
            return false;
        }

        // 2. Проверяем пересечения с записями
        if (hasTimeOverlap(startTime, endTime, appointments)) {
            return false;
        }

        return true;
    }

    private boolean isOnClientSlotBoundary(LocalDateTime time, LocalTime workStart) {
        LocalTime localTime = time.toLocalTime();

        // Проверяем, что время соответствует шагу 2.5 часа от начала рабочего дня
        int totalMinutes = localTime.getHour() * 60 + localTime.getMinute();
        int startMinutes = workStart.getHour() * 60 + workStart.getMinute();

        return (totalMinutes - startMinutes) % 150 == 0; // 150 минут = 2.5 часа
    }

    private boolean isTimeBlocked(LocalDateTime start, LocalDateTime end, List<ScheduleBlock> blocks) {
        return blocks.stream()
                .filter(ScheduleBlock::getIsBlocked)
                .anyMatch(block ->
                        start.isBefore(block.getEndTime()) &&
                                end.isAfter(block.getStartTime())
                );
    }

    private boolean hasTimeOverlap(LocalDateTime start, LocalDateTime end, List<Appointment> appointments) {
        return appointments.stream().anyMatch(appointment ->
                start.isBefore(appointment.getEndTime()) &&
                        end.isAfter(appointment.getStartTime())
        );
    }

    private List<Appointment> getAppointmentsForDate(LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

        return appointmentRepository.findByDate(startOfDay, endOfDay);
    }

    private List<ScheduleBlock> getBlocksForDate(LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

        return scheduleBlockRepository.findBlocksInRange(startOfDay, endOfDay);
    }

    private List<TimeRange> findAvailableTimeRanges(LocalDateTime dayStart, LocalDateTime dayEnd,
                                                    List<Appointment> appointments,
                                                    List<ScheduleBlock> blocks,
                                                    Integer minDuration) {
        List<TimeRange> ranges = new ArrayList<>();
        LocalDateTime current = dayStart;

        // Сортируем все "занятые" события (записи + блокировки)
        List<TimeEvent> events = new ArrayList<>();
        appointments.forEach(a -> events.add(new TimeEvent(a.getStartTime(), a.getEndTime(), true)));
        blocks.stream()
                .filter(ScheduleBlock::getIsBlocked)
                .forEach(b -> events.add(new TimeEvent(b.getStartTime(), b.getEndTime(), true)));

        events.sort(Comparator.comparing(TimeEvent::getStartTime));

        // Находим свободные промежутки
        for (TimeEvent event : events) {
            if (current.isBefore(event.getStartTime())) {
                long freeMinutes = java.time.Duration.between(current, event.getStartTime()).toMinutes();
                if (freeMinutes >= minDuration) {
                    ranges.add(new TimeRange(current, event.getStartTime()));
                }
            }

            // Перемещаемся после события
            if (current.isBefore(event.getEndTime())) {
                current = event.getEndTime();
            }
        }

        // Последний промежуток до конца дня
        if (current.isBefore(dayEnd)) {
            long freeMinutes = java.time.Duration.between(current, dayEnd).toMinutes();
            if (freeMinutes >= minDuration) {
                ranges.add(new TimeRange(current, dayEnd));
            }
        }

        return ranges;
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ КЛАССЫ ==========

    @Data
    @RequiredArgsConstructor
    public static class TimeRange {
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;

        public long getDurationMinutes() {
            return java.time.Duration.between(startTime, endTime).toMinutes();
        }
    }

    @Data
    @RequiredArgsConstructor
    private static class TimeEvent {
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;
        private final boolean isOccupied;
    }
}