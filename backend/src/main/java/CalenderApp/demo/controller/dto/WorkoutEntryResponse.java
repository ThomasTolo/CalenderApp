package CalenderApp.demo.controller.dto;

import java.math.BigDecimal;

public record WorkoutEntryResponse(
        Long exerciseId,
        String exerciseName,
        int sets,
        int reps,
        BigDecimal weight
) {
}
