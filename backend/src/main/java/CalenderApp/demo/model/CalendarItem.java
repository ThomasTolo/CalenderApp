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
import java.time.LocalDate;
import java.time.LocalTime;
import java.math.BigDecimal;

@Entity
@Table(
        name = "calendar_items",
        indexes = {
                @Index(name = "idx_calendar_items_user_date", columnList = "user_id,date"),
                @Index(name = "idx_calendar_items_date", columnList = "date")
        }
)
public class CalendarItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false)
    private LocalDate date;

    private LocalTime startTime;

    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private CalendarItemType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ImportanceLevel importance = ImportanceLevel.MEDIUM;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(length = 2000)
    private String log;

    @Column(nullable = false)
    private boolean done = false;

    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private SchoolItemKind schoolKind;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fixed_cost_subscription_id")
    private FixedCostSubscription fixedCostSubscription;

    @Column(nullable = false)
    private boolean notified = false;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    protected CalendarItem() {
    }

    public CalendarItem(AppUser user, LocalDate date, CalendarItemType type, String title) {
        this.user = user;
        this.date = date;
        this.type = type;
        this.title = title;
    }

    public Long getId() {
        return id;
    }

    public AppUser getUser() {
        return user;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
        touch();
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
        touch();
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
        touch();
    }

    public CalendarItemType getType() {
        return type;
    }

    public void setType(CalendarItemType type) {
        this.type = type;
        touch();
    }

    public ImportanceLevel getImportance() {
        return importance;
    }

    public void setImportance(ImportanceLevel importance) {
        this.importance = importance;
        touch();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        touch();
    }

    public String getLog() {
        return log;
    }

    public void setLog(String log) {
        this.log = log;
        touch();
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
        touch();
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
        touch();
    }

    public SchoolItemKind getSchoolKind() {
        return schoolKind;
    }

    public void setSchoolKind(SchoolItemKind schoolKind) {
        this.schoolKind = schoolKind;
        touch();
    }

    public FixedCostSubscription getFixedCostSubscription() {
        return fixedCostSubscription;
    }

    public void setFixedCostSubscription(FixedCostSubscription fixedCostSubscription) {
        this.fixedCostSubscription = fixedCostSubscription;
        touch();
    }

    public boolean isNotified() {
        return notified;
    }

    public void setNotified(boolean notified) {
        this.notified = notified;
        touch();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }
}
