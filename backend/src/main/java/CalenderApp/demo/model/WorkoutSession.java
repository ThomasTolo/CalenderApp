package CalenderApp.demo.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "workout_sessions",
        indexes = {
                @Index(name = "idx_workout_sessions_user", columnList = "user_id"),
                @Index(name = "idx_workout_sessions_calendar_item", columnList = "calendar_item_id")
        }
)
public class WorkoutSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "calendar_item_id", nullable = false, unique = true)
    private CalendarItem calendarItem;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<WorkoutSessionEntry> entries = new ArrayList<>();

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    protected WorkoutSession() {
    }

    public WorkoutSession(AppUser user, CalendarItem calendarItem) {
        this.user = user;
        this.calendarItem = calendarItem;
    }

    public Long getId() {
        return id;
    }

    public AppUser getUser() {
        return user;
    }

    public CalendarItem getCalendarItem() {
        return calendarItem;
    }

    public List<WorkoutSessionEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<WorkoutSessionEntry> entries) {
        this.entries = entries;
        touch();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }
}
