package main.java.CalenderApp.demo.service.impl;

import CalenderApp.demo.model.CalendarItem;
import CalenderApp.demo.model.Notification;
import CalenderApp.demo.model.NotificationType;
import CalenderApp.demo.repository.CalendarItemRepository;
import CalenderApp.demo.service.NotificationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;

@Component
public class UpcomingNotificationScheduler {

    private final CalendarItemRepository itemRepository;
    private final NotificationService notificationService;

    public UpcomingNotificationScheduler(CalendarItemRepository itemRepository, NotificationService notificationService) {
        this.itemRepository = itemRepository;
        this.notificationService = notificationService;
    }

    // Every minute
    @Scheduled(fixedDelayString = "${app.notifications.upcoming.pollMs:60000}")
    public void notifyUpcoming() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        LocalTime soon = now.plusMinutes(10);

        for (CalendarItem item : itemRepository.findByDateAndNotifiedFalseAndStartTimeIsNotNull(today)) {
            LocalTime start = item.getStartTime();
            if (start == null) {
                continue;
            }
            if (!start.isBefore(now) && !start.isAfter(soon)) {
                item.setNotified(true);
                itemRepository.save(item);

                notificationService.create(new Notification(
                        item.getUser(),
                        NotificationType.UPCOMING,
                        item.getImportance(),
                        "Upcoming " + item.getType() + " at " + start + ": " + item.getTitle(),
                        item.getId()
                ));
            }
        }
    }
}
