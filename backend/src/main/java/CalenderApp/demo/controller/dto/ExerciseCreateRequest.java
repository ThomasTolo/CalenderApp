package CalenderApp.demo.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExerciseCreateRequest(
        @NotBlank @Size(max = 80) String name
) {
}
