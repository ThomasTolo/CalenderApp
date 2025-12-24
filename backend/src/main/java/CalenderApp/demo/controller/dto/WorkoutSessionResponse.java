package CalenderApp.demo.controller.dto;

import java.time.Instant;
import java.util.List;

public record WorkoutSessionResponse(
        Long calendarItemId,
        List<WorkoutEntryResponse> entries,
        Instant createdAt,
        Instant updatedAt
) {
}
