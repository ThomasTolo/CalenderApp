package CalenderApp.demo.service;

import CalenderApp.demo.model.AppUser;
import CalenderApp.demo.model.Exercise;
import CalenderApp.demo.model.WorkoutSession;
import CalenderApp.demo.model.WorkoutTemplate;

import java.math.BigDecimal;
import java.util.List;

public interface WorkoutService {

    List<Exercise> listExercises(AppUser user);

    Exercise createExercise(AppUser user, String name);

    void deleteExercise(AppUser user, Long exerciseId);

    List<WorkoutTemplate> listTemplates(AppUser user);

    WorkoutTemplate createTemplate(AppUser user, String title, List<EntrySpec> entries);

    WorkoutTemplate updateTemplate(AppUser user, Long templateId, String title, List<EntrySpec> entries);

    void deleteTemplate(AppUser user, Long templateId);

    WorkoutSession getOrCreateSession(AppUser user, Long calendarItemId);

    WorkoutSession updateSession(AppUser user, Long calendarItemId, List<EntrySpec> entries);

    record EntrySpec(Long exerciseId, int sets, int reps, BigDecimal weight) {
    }
}
