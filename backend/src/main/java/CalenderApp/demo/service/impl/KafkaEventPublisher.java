package main.java.CalenderApp.demo.service.impl;

import CalenderApp.demo.config.RawWebSocketServer;
import CalenderApp.demo.model.CalendarItem;
import CalenderApp.demo.model.Notification;
import CalenderApp.demo.model.NotificationType;
import CalenderApp.demo.service.EventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class KafkaEventPublisher implements EventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final String calendarTopic;
    private final String notificationTopic;

    public KafkaEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${app.kafka.topic.calendar:calendar.events}") String calendarTopic,
            @Value("${app.kafka.topic.notification:calendar.notifications}") String notificationTopic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.calendarTopic = calendarTopic;
        this.notificationTopic = notificationTopic;
    }

    @Override
    public void calendarItemEvent(NotificationType type, CalendarItem item) {
        CalendarItemEvent event = new CalendarItemEvent(
                UUID.randomUUID().toString(),
                type,
                item.getUser().getId(),
                item.getId(),
                item.getDate().toString(),
                Instant.now().toString()
        );

        publish(calendarTopic, "user-" + item.getUser().getId(), event);
        RawWebSocketServer.broadcastJson(event);
    }

    @Override
    public void notificationCreated(Notification notification) {
        NotificationEvent event = new NotificationEvent(
                UUID.randomUUID().toString(),
                notification.getType(),
                notification.getUser().getId(),
                notification.getId(),
                notification.getCalendarItemId(),
                notification.getMessage(),
                notification.getCreatedAt().toString()
        );

        publish(notificationTopic, "user-" + notification.getUser().getId(), event);
        RawWebSocketServer.broadcastJson(event);
    }

    private void publish(String topic, String key, Object payload) {
        try {
            kafkaTemplate.send(topic, key, mapper.writeValueAsString(payload));
        } catch (Exception ignored) {
            // Local dev may not have Kafka running; don't crash core CRUD.
        }
    }

    public record CalendarItemEvent(
            String eventId,
            NotificationType type,
            Long userId,
            Long itemId,
            String date,
            String occurredAt
    ) {
    }

    public record NotificationEvent(
            String eventId,
            NotificationType type,
            Long userId,
            Long notificationId,
            Long calendarItemId,
            String message,
            String createdAt
    ) {
    }
}
