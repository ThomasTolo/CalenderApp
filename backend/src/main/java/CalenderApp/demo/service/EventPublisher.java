package CalenderApp.demo.service;

import CalenderApp.demo.model.CalendarItem;
import CalenderApp.demo.model.Notification;
import CalenderApp.demo.model.NotificationType;

public interface EventPublisher {
    void calendarItemEvent(NotificationType type, CalendarItem item);

    void notificationCreated(Notification notification);
}
