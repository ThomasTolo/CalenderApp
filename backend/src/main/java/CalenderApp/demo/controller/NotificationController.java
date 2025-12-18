package CalenderApp.demo.controller;

import CalenderApp.demo.controller.dto.NotificationResponse;
import CalenderApp.demo.model.AppUser;
import CalenderApp.demo.service.CurrentUserService;
import CalenderApp.demo.service.NotificationService;
import CalenderApp.demo.service.view.NotificationView;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentUserService currentUserService;

    public NotificationController(NotificationService notificationService, CurrentUserService currentUserService) {
        this.notificationService = notificationService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/unread")
    public List<NotificationResponse> unread(Principal principal) {
        AppUser user = currentUserService.requireByUsername(principal.getName());
        return notificationService.listUnread(user).stream().map(NotificationController::toResponse).toList();
    }

    @GetMapping
    public List<NotificationResponse> all(Principal principal) {
        AppUser user = currentUserService.requireByUsername(principal.getName());
        return notificationService.listAll(user).stream().map(NotificationController::toResponse).toList();
    }

    @PostMapping("/{id}/read")
    public NotificationResponse markRead(@PathVariable Long id, Principal principal) {
        AppUser user = currentUserService.requireByUsername(principal.getName());
        return toResponse(notificationService.markRead(user, id));
    }

    private static NotificationResponse toResponse(NotificationView view) {
        return new NotificationResponse(
                view.id(),
                view.type(),
                view.importance(),
                view.message(),
                view.calendarItemId(),
                view.read(),
                view.createdAt()
        );
    }
}
