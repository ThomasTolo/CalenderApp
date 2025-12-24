package CalenderApp.demo.service.impl;

import CalenderApp.demo.model.CalendarItem;
import CalenderApp.demo.model.CalendarItemType;
import CalenderApp.demo.model.Notification;
import CalenderApp.demo.model.NotificationType;
import CalenderApp.demo.repository.CalendarItemRepository;
import CalenderApp.demo.service.NotificationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class FixedCostDueNotificationScheduler {

    private final CalendarItemRepository itemRepository;
    private final NotificationService notificationService;

    public FixedCostDueNotificationScheduler(CalendarItemRepository itemRepository, NotificationService notificationService) {
        this.itemRepository = itemRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "0 0 9 * * *")
    public void notifyFixedCostsDueToday() {
        LocalDate today = LocalDate.now();
        for (CalendarItem item : itemRepository.findByDateAndTypeAndNotifiedFalse(today, CalendarItemType.FIXED_COST)) {
            item.setNotified(true);
            itemRepository.save(item);

            notificationService.create(new Notification(
                    item.getUser(),
                    NotificationType.UPCOMING,
                    item.getImportance(),
                    "Fixed cost due today: " + item.getTitle() + (item.getAmount() != null ? (" (" + item.getAmount() + ")") : ""),
                    item.getId()
            ));
        }
    }
}
