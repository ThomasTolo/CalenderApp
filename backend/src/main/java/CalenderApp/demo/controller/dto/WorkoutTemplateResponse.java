package CalenderApp.demo.controller.dto;

import java.time.Instant;
import java.util.List;

public record WorkoutTemplateResponse(
        Long id,
        String title,
        List<WorkoutEntryResponse> entries,
        Instant createdAt,
        Instant updatedAt
) {
}
