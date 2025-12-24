package CalenderApp.demo.service.view;

import CalenderApp.demo.model.CalendarItemType;
import CalenderApp.demo.model.FixedCostFrequency;
import CalenderApp.demo.model.ImportanceLevel;
import CalenderApp.demo.model.SchoolItemKind;

import java.math.BigDecimal;
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
        boolean done,
        BigDecimal amount,
        SchoolItemKind schoolKind,
        FixedCostFrequency fixedCostFrequency,
        Instant createdAt,
        Instant updatedAt
) {
}
