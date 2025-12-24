package CalenderApp.demo.controller.dto;

import CalenderApp.demo.model.CalendarItemType;
import CalenderApp.demo.model.FixedCostFrequency;
import CalenderApp.demo.model.ImportanceLevel;
import CalenderApp.demo.model.SchoolItemKind;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record CalendarItemCreateRequest(
        @NotNull LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        @NotNull CalendarItemType type,
        @NotNull ImportanceLevel importance,
        @NotBlank @Size(max = 120) String title,
        @Size(max = 2000) String log,
        Boolean done,
        @DecimalMin("0.0") BigDecimal amount,
        SchoolItemKind schoolKind,
        FixedCostFrequency fixedCostFrequency
) {
}
