package CalenderApp.demo.controller.dto;

import CalenderApp.demo.model.CalendarItemType;
import CalenderApp.demo.model.ImportanceLevel;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record CalendarItemResponse(
        Long id,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        CalendarItemType type,
        ImportanceLevel importance,
        String title,
        String log,
        Instant createdAt,
        Instant updatedAt
) {
}
