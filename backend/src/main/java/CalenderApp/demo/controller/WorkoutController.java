package CalenderApp.demo.controller;

import CalenderApp.demo.controller.dto.ExerciseCreateRequest;
import CalenderApp.demo.controller.dto.ExerciseResponse;
import CalenderApp.demo.controller.dto.WorkoutEntryRequest;
import CalenderApp.demo.controller.dto.WorkoutEntryResponse;
import CalenderApp.demo.controller.dto.WorkoutSessionResponse;
import CalenderApp.demo.controller.dto.WorkoutSessionUpdateRequest;
import CalenderApp.demo.controller.dto.WorkoutTemplateCreateRequest;
import CalenderApp.demo.controller.dto.WorkoutTemplateResponse;
import CalenderApp.demo.model.AppUser;
import CalenderApp.demo.model.Exercise;
import CalenderApp.demo.model.WorkoutSession;
import CalenderApp.demo.model.WorkoutSessionEntry;
import CalenderApp.demo.model.WorkoutTemplate;
import CalenderApp.demo.model.WorkoutTemplateEntry;
import CalenderApp.demo.service.CurrentUserService;
import CalenderApp.demo.service.WorkoutService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/workout")
public class WorkoutController {

    private final CurrentUserService currentUserService;
    private final WorkoutService workoutService;

    public WorkoutController(CurrentUserService currentUserService, WorkoutService workoutService) {
        this.currentUserService = currentUserService;
        this.workoutService = workoutService;
    }

    @GetMapping("/exercises")
    public List<ExerciseResponse> listExercises(Principal principal) {
        AppUser user = currentUserService.requireByUsername(principal.getName());
        return workoutService.listExercises(user).stream().map(WorkoutController::toExerciseResponse).toList();
    }

    @PostMapping("/exercises")
    public ExerciseResponse createExercise(@Valid @RequestBody ExerciseCreateRequest request, Principal principal) {
        AppUser user = currentUserService.requireByUsername(principal.getName());
        Exercise created = workoutService.createExercise(user, request.name());
        return toExerciseResponse(created);
    }

    @DeleteMapping("/exercises/{id}")
    public void deleteExercise(@PathVariable Long id, Principal principal) {
        AppUser user = currentUserService.requireByUsername(principal.getName());
        workoutService.deleteExercise(user, id);
    }

    @GetMapping("/templates")
    public List<WorkoutTemplateResponse> listTemplates(Principal principal) {
        AppUser user = currentUserService.requireByUsername(principal.getName());
        return workoutService.listTemplates(user).stream().map(WorkoutController::toTemplateResponse).toList();
    }

    @PostMapping("/templates")
    public WorkoutTemplateResponse createTemplate(@Valid @RequestBody WorkoutTemplateCreateRequest request, Principal principal) {
        AppUser user = currentUserService.requireByUsername(principal.getName());
        WorkoutTemplate template = workoutService.createTemplate(user, request.title(), request.entries().stream().map(WorkoutController::toSpec).toList());
        return toTemplateResponse(template);
    }

    @PutMapping("/templates/{id}")
    public WorkoutTemplateResponse updateTemplate(@PathVariable Long id, @Valid @RequestBody WorkoutTemplateCreateRequest request, Principal principal) {
        AppUser user = currentUserService.requireByUsername(principal.getName());
        WorkoutTemplate template = workoutService.updateTemplate(user, id, request.title(), request.entries().stream().map(WorkoutController::toSpec).toList());
        return toTemplateResponse(template);
    }

    @DeleteMapping("/templates/{id}")
    public void deleteTemplate(@PathVariable Long id, Principal principal) {
        AppUser user = currentUserService.requireByUsername(principal.getName());
        workoutService.deleteTemplate(user, id);
    }

    @GetMapping("/sessions/{calendarItemId}")
    public WorkoutSessionResponse getSession(@PathVariable Long calendarItemId, Principal principal) {
        AppUser user = currentUserService.requireByUsername(principal.getName());
        WorkoutSession session = workoutService.getOrCreateSession(user, calendarItemId);
        return toSessionResponse(calendarItemId, session);
    }

    @PutMapping("/sessions/{calendarItemId}")
    public WorkoutSessionResponse updateSession(@PathVariable Long calendarItemId, @Valid @RequestBody WorkoutSessionUpdateRequest request, Principal principal) {
        AppUser user = currentUserService.requireByUsername(principal.getName());
        WorkoutSession session = workoutService.updateSession(user, calendarItemId, request.entries().stream().map(WorkoutController::toSpec).toList());
        return toSessionResponse(calendarItemId, session);
    }

    private static ExerciseResponse toExerciseResponse(Exercise ex) {
        return new ExerciseResponse(ex.getId(), ex.getName());
    }

    private static WorkoutTemplateResponse toTemplateResponse(WorkoutTemplate template) {
        List<WorkoutEntryResponse> entries = template.getEntries().stream().map(WorkoutController::toEntryResponse).toList();
        return new WorkoutTemplateResponse(template.getId(), template.getTitle(), entries, template.getCreatedAt(), template.getUpdatedAt());
    }

    private static WorkoutSessionResponse toSessionResponse(Long calendarItemId, WorkoutSession session) {
        List<WorkoutEntryResponse> entries = session.getEntries().stream().map(WorkoutController::toEntryResponse).toList();
        return new WorkoutSessionResponse(calendarItemId, entries, session.getCreatedAt(), session.getUpdatedAt());
    }

    private static WorkoutEntryResponse toEntryResponse(WorkoutTemplateEntry entry) {
        return new WorkoutEntryResponse(entry.getExercise().getId(), entry.getExercise().getName(), entry.getSets(), entry.getReps(), entry.getWeight());
    }

    private static WorkoutEntryResponse toEntryResponse(WorkoutSessionEntry entry) {
        return new WorkoutEntryResponse(entry.getExercise().getId(), entry.getExercise().getName(), entry.getSets(), entry.getReps(), entry.getWeight());
    }

    private static WorkoutService.EntrySpec toSpec(WorkoutEntryRequest req) {
        return new WorkoutService.EntrySpec(req.exerciseId(), req.sets(), req.reps(), req.weight());
    }
}
