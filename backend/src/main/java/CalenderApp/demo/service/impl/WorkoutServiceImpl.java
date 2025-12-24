package CalenderApp.demo.service.impl;

import CalenderApp.demo.model.AppUser;
import CalenderApp.demo.model.CalendarItem;
import CalenderApp.demo.model.CalendarItemType;
import CalenderApp.demo.model.Exercise;
import CalenderApp.demo.model.WorkoutSession;
import CalenderApp.demo.model.WorkoutSessionEntry;
import CalenderApp.demo.model.WorkoutTemplate;
import CalenderApp.demo.model.WorkoutTemplateEntry;
import CalenderApp.demo.repository.CalendarItemRepository;
import CalenderApp.demo.repository.ExerciseRepository;
import CalenderApp.demo.repository.WorkoutSessionRepository;
import CalenderApp.demo.repository.WorkoutTemplateRepository;
import CalenderApp.demo.service.WorkoutService;
import CalenderApp.demo.service.exception.BadRequestException;
import CalenderApp.demo.service.exception.NotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WorkoutServiceImpl implements WorkoutService {

    private final ExerciseRepository exerciseRepository;
    private final WorkoutTemplateRepository templateRepository;
    private final WorkoutSessionRepository sessionRepository;
    private final CalendarItemRepository calendarItemRepository;

    public WorkoutServiceImpl(
            ExerciseRepository exerciseRepository,
            WorkoutTemplateRepository templateRepository,
            WorkoutSessionRepository sessionRepository,
            CalendarItemRepository calendarItemRepository
    ) {
        this.exerciseRepository = exerciseRepository;
        this.templateRepository = templateRepository;
        this.sessionRepository = sessionRepository;
        this.calendarItemRepository = calendarItemRepository;
    }

    @Override
    public List<Exercise> listExercises(AppUser user) {
        return exerciseRepository.findByUserOrderByNameAsc(user);
    }

    @Override
    public Exercise createExercise(AppUser user, String name) {
        if (exerciseRepository.existsByUserAndNameIgnoreCase(user, name)) {
            throw new BadRequestException("exercise already exists");
        }
        return exerciseRepository.save(new Exercise(user, name.trim()));
    }

    @Override
    public void deleteExercise(AppUser user, Long exerciseId) {
        Exercise ex = exerciseRepository.findByIdAndUser(exerciseId, user)
                .orElseThrow(() -> new NotFoundException("Exercise not found"));
        try {
            exerciseRepository.delete(java.util.Objects.requireNonNull(ex));
        } catch (DataIntegrityViolationException e) {
            throw new BadRequestException("Exercise is used by a template/session");
        }
    }

    @Override
    public List<WorkoutTemplate> listTemplates(AppUser user) {
        return templateRepository.findByUserOrderByUpdatedAtDesc(user);
    }

    @Override
    public WorkoutTemplate createTemplate(AppUser user, String title, List<EntrySpec> entries) {
        WorkoutTemplate template = new WorkoutTemplate(user, title.trim());
        applyTemplateEntries(user, template, entries);
        return templateRepository.save(template);
    }

    @Override
    public WorkoutTemplate updateTemplate(AppUser user, Long templateId, String title, List<EntrySpec> entries) {
        WorkoutTemplate template = templateRepository.findByIdAndUser(templateId, user)
                .orElseThrow(() -> new NotFoundException("Template not found"));
        template.setTitle(title.trim());
        applyTemplateEntries(user, template, entries);
        return templateRepository.save(template);
    }

    @Override
    public void deleteTemplate(AppUser user, Long templateId) {
        WorkoutTemplate template = templateRepository.findByIdAndUser(templateId, user)
                .orElseThrow(() -> new NotFoundException("Template not found"));
        templateRepository.delete(java.util.Objects.requireNonNull(template));
    }

    @Override
    public WorkoutSession getOrCreateSession(AppUser user, Long calendarItemId) {
        CalendarItem item = requireWorkoutCalendarItem(user, calendarItemId);
        return sessionRepository.findByUserAndCalendarItem_Id(user, calendarItemId)
                .orElseGet(() -> sessionRepository.save(new WorkoutSession(user, item)));
    }

    @Override
    public WorkoutSession updateSession(AppUser user, Long calendarItemId, List<EntrySpec> entries) {
        CalendarItem item = requireWorkoutCalendarItem(user, calendarItemId);
        WorkoutSession session = sessionRepository.findByUserAndCalendarItem_Id(user, calendarItemId)
                .orElseGet(() -> new WorkoutSession(user, item));

        session.getEntries().clear();
        int pos = 0;
        for (EntrySpec spec : entries) {
            Exercise ex = exerciseRepository.findByIdAndUser(spec.exerciseId(), user)
                    .orElseThrow(() -> new BadRequestException("Unknown exerciseId: " + spec.exerciseId()));
            session.getEntries().add(new WorkoutSessionEntry(session, ex, spec.sets(), spec.reps(), spec.weight(), pos++));
        }
        session.touch();
        return sessionRepository.save(session);
    }

    private CalendarItem requireWorkoutCalendarItem(AppUser user, Long calendarItemId) {
        CalendarItem item = calendarItemRepository.findByIdAndUser(calendarItemId, user)
                .orElseThrow(() -> new NotFoundException("Calendar item not found"));
        if (item.getType() != CalendarItemType.WORKOUT) {
            throw new BadRequestException("Calendar item is not WORKOUT");
        }
        return item;
    }

    private void applyTemplateEntries(AppUser user, WorkoutTemplate template, List<EntrySpec> entries) {
        template.getEntries().clear();
        int pos = 0;
        for (EntrySpec spec : entries) {
            Exercise ex = exerciseRepository.findByIdAndUser(spec.exerciseId(), user)
                    .orElseThrow(() -> new BadRequestException("Unknown exerciseId: " + spec.exerciseId()));
            template.getEntries().add(new WorkoutTemplateEntry(template, ex, spec.sets(), spec.reps(), spec.weight(), pos++));
        }
    }
}
