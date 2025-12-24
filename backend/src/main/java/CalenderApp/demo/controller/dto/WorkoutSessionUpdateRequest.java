package CalenderApp.demo.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record WorkoutSessionUpdateRequest(
        @NotNull @Valid List<WorkoutEntryRequest> entries
) {
}
