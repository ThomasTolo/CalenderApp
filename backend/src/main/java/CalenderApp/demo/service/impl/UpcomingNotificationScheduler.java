package CalenderApp.demo.service.impl;

import CalenderApp.demo.model.CalendarItem;
import CalenderApp.demo.model.Notification;
import CalenderApp.demo.model.NotificationType;
import CalenderApp.demo.repository.CalendarItemRepository;
import CalenderApp.demo.service.NotificationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

@Component
public class UpcomingNotificationScheduler {

    private final CalendarItemRepository itemRepository;
    private final NotificationService notificationService;

    public UpcomingNotificationScheduler(CalendarItemRepository itemRepository, NotificationService notificationService) {
        this.itemRepository = itemRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(fixedDelayString = "${app.notifications.upcoming.pollMs:60000}")
    public void notifyUpcoming() {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        LocalDateTime now = LocalDateTime.now();

        for (CalendarItem item : itemRepository.findByDateBetweenAndNotifiedFalseAndStartTimeIsNotNull(today, tomorrow)) {
            LocalTime startTime = item.getStartTime();
            if (startTime == null) {
                continue;
            }

            long leadMinutes = leadMinutes(item);
            LocalDateTime start = LocalDateTime.of(item.getDate(), startTime);
            long minutesUntil = TimeUnit.SECONDS.toMinutes(java.time.Duration.between(now, start).getSeconds());

            if (minutesUntil < 0 || minutesUntil > leadMinutes) {
                continue;
            }

            item.setNotified(true);
            itemRepository.save(item);

            notificationService.create(new Notification(
                    item.getUser(),
                    NotificationType.UPCOMING,
                    item.getImportance(),
                    "Upcoming " + item.getType() + " at " + startTime + ": " + item.getTitle(),
                    item.getId()
            ));
        }
    }

    private static long leadMinutes(CalendarItem item) {
        return switch (item.getType()) {
            case JOB -> 24 * 60L;
            case SCHOOL -> 2 * 60L;
            case FIXED_COST -> 24 * 60L;
            default -> 10L;
        };
    }
}
