package CalenderApp.demo.service.command;

import CalenderApp.demo.model.CalendarItemType;
import CalenderApp.demo.model.ImportanceLevel;

import java.time.LocalDate;
import java.time.LocalTime;

public record UpdateCalendarItemCommand(
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        CalendarItemType type,
        ImportanceLevel importance,
        String title,
        String log
) {
}
