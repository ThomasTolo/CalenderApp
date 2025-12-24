package CalenderApp.demo.service.impl;

import CalenderApp.demo.model.AppUser;
import CalenderApp.demo.model.CalendarItem;
import CalenderApp.demo.model.CalendarItemType;
import CalenderApp.demo.model.ImportanceLevel;
import CalenderApp.demo.model.Notification;
import CalenderApp.demo.model.NotificationType;
import CalenderApp.demo.repository.AppUserRepository;
import CalenderApp.demo.repository.CalendarItemRepository;
import CalenderApp.demo.service.NotificationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class WorkoutInactivityNotificationScheduler {

    private final AppUserRepository userRepository;
    private final CalendarItemRepository itemRepository;
    private final NotificationService notificationService;

    public WorkoutInactivityNotificationScheduler(
            AppUserRepository userRepository,
            CalendarItemRepository itemRepository,
            NotificationService notificationService
    ) {
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
        this.notificationService = notificationService;
    }

    // Weekly check (Monday 09:00)
    @Scheduled(cron = "0 0 9 * * MON")
    public void notifyIfNoWorkoutForAWeek() {
        LocalDate today = LocalDate.now();
        LocalDate cutoff = today.minusDays(7);

        for (AppUser user : userRepository.findAll()) {
            java.util.Optional<CalendarItem> last = itemRepository.findTopByUserAndTypeAndDoneTrueOrderByDateDesc(user, CalendarItemType.WORKOUT);
            if (last.isPresent() && !last.get().getDate().isBefore(cutoff)) {
                continue;
            }

            String message = last
                    .map(ci -> "No workout completed in 7 days (last: " + ci.getDate() + ")")
                    .orElse("No workout completed in 7 days");

            notificationService.create(new Notification(
                    user,
                    NotificationType.UPCOMING,
                    ImportanceLevel.MEDIUM,
                    message,
                    last.map(CalendarItem::getId).orElse(null)
            ));
        }
    }
}
