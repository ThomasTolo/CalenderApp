package main.java.CalenderApp.demo.service.impl;

import CalenderApp.demo.model.AppUser;
import CalenderApp.demo.model.Notification;
import CalenderApp.demo.repository.NotificationRepository;
import CalenderApp.demo.service.EventPublisher;
import CalenderApp.demo.service.NotificationService;
import CalenderApp.demo.service.exception.NotFoundException;
import CalenderApp.demo.service.view.NotificationView;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final EventPublisher eventPublisher;

    public NotificationServiceImpl(NotificationRepository notificationRepository, EventPublisher eventPublisher) {
        this.notificationRepository = notificationRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public List<NotificationView> listUnread(AppUser user) {
        return notificationRepository.findByUserAndReadFalseOrderByCreatedAtDesc(user)
                .stream()
                .map(NotificationServiceImpl::toView)
                .toList();
    }

    @Override
    public List<NotificationView> listAll(AppUser user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(NotificationServiceImpl::toView)
                .toList();
    }

    @Override
    public NotificationView markRead(AppUser user, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndUser(notificationId, user)
                .orElseThrow(() -> new NotFoundException("Notification not found"));
        notification.markRead();
        notificationRepository.save(notification);
        return toView(notification);
    }

    @Override
    public NotificationView create(Notification notification) {
        Notification saved = notificationRepository.save(notification);
        eventPublisher.notificationCreated(saved);
        return toView(saved);
    }

    private static NotificationView toView(Notification notification) {
        return new NotificationView(
                notification.getId(),
                notification.getType(),
                notification.getImportance(),
                notification.getMessage(),
                notification.getCalendarItemId(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
