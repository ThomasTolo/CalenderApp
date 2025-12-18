package CalenderApp.demo.service.view;

import CalenderApp.demo.model.ImportanceLevel;
import CalenderApp.demo.model.NotificationType;

import java.time.Instant;

public record NotificationView(
        Long id,
        NotificationType type,
        ImportanceLevel importance,
        String message,
        Long calendarItemId,
        boolean read,
        Instant createdAt
) {
}
