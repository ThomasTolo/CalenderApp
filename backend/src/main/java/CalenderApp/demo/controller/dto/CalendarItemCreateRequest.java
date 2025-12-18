package CalenderApp.demo.controller.dto;

import CalenderApp.demo.model.CalendarItemType;
import CalenderApp.demo.model.ImportanceLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record CalendarItemCreateRequest(
        @NotNull LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        @NotNull CalendarItemType type,
        @NotNull ImportanceLevel importance,
        @NotBlank @Size(max = 120) String title,
        @Size(max = 2000) String log
) {
}
