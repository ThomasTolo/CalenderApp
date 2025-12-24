package CalenderApp.demo.controller.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record WorkoutEntryRequest(
        @NotNull Long exerciseId,
        @Min(1) int sets,
        @Min(1) int reps,
        @DecimalMin("0.0") BigDecimal weight
) {
}
