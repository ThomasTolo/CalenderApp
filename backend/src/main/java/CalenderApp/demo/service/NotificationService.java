package CalenderApp.demo.service;

import CalenderApp.demo.model.AppUser;
import CalenderApp.demo.model.Notification;
import CalenderApp.demo.service.view.NotificationView;

import java.util.List;

public interface NotificationService {
    List<NotificationView> listUnread(AppUser user);

    List<NotificationView> listAll(AppUser user);

    NotificationView markRead(AppUser user, Long notificationId);

    NotificationView create(Notification notification);
}
