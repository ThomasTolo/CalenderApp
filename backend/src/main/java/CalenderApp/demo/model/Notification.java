package CalenderApp.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(
        name = "notifications",
        indexes = {
                @Index(name = "idx_notifications_user_read", columnList = "user_id,read"),
                @Index(name = "idx_notifications_created_at", columnList = "createdAt")
        }
)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ImportanceLevel importance = ImportanceLevel.MEDIUM;

    @Column(nullable = false, length = 280)
    private String message;

    private Long calendarItemId;

    @Column(nullable = false)
    private boolean read = false;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected Notification() {
    }

    public Notification(AppUser user, NotificationType type, ImportanceLevel importance, String message, Long calendarItemId) {
        this.user = user;
        this.type = type;
        this.importance = importance;
        this.message = message;
        this.calendarItemId = calendarItemId;
    }

    public Long getId() {
        return id;
    }

    public AppUser getUser() {
        return user;
    }

    public NotificationType getType() {
        return type;
    }

    public ImportanceLevel getImportance() {
        return importance;
    }

    public String getMessage() {
        return message;
    }

    public Long getCalendarItemId() {
        return calendarItemId;
    }

    public boolean isRead() {
        return read;
    }

    public void markRead() {
        this.read = true;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
