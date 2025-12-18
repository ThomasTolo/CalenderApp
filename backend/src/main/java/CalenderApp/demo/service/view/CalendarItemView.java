package CalenderApp.demo.service.view;

import CalenderApp.demo.model.CalendarItemType;
import CalenderApp.demo.model.ImportanceLevel;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record CalendarItemView(
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
