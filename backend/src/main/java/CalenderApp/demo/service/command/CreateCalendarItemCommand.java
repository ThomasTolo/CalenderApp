package CalenderApp.demo.service.command;

import CalenderApp.demo.model.CalendarItemType;
import CalenderApp.demo.model.FixedCostFrequency;
import CalenderApp.demo.model.ImportanceLevel;
import CalenderApp.demo.model.SchoolItemKind;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record CreateCalendarItemCommand(
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        CalendarItemType type,
        ImportanceLevel importance,
        String title,
        String log,
        Boolean done,
        BigDecimal amount,
        SchoolItemKind schoolKind,
        FixedCostFrequency fixedCostFrequency
) {
}
