package main.java.CalenderApp.demo.controller.dto;

import CalenderApp.demo.model.ImportanceLevel;
import CalenderApp.demo.model.NotificationType;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        NotificationType type,
        ImportanceLevel importance,
        String message,
        Long calendarItemId,
        boolean read,
        Instant createdAt
) {
}
