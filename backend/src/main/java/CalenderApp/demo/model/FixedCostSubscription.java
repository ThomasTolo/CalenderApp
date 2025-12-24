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

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "fixed_cost_subscriptions",
        indexes = {
                @Index(name = "idx_fixed_cost_subscriptions_user_active", columnList = "user_id,active")
        }
)
public class FixedCostSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private int dayOfMonth;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private FixedCostFrequency frequency;

    @Column
    private Integer dayOfWeek;

    @Column
    private Integer monthOfYear;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected FixedCostSubscription() {
    }

    public FixedCostSubscription(AppUser user, String title, BigDecimal amount, int dayOfMonth) {
        this.user = user;
        this.title = title;
        this.amount = amount;
        this.dayOfMonth = dayOfMonth;
        this.frequency = FixedCostFrequency.MONTHLY;
        this.dayOfWeek = null;
        this.monthOfYear = null;
    }

    public FixedCostSubscription(
            AppUser user,
            String title,
            BigDecimal amount,
            FixedCostFrequency frequency,
            int dayOfMonth,
            Integer dayOfWeek,
            Integer monthOfYear
    ) {
        this.user = user;
        this.title = title;
        this.amount = amount;
        this.frequency = frequency;
        this.dayOfMonth = dayOfMonth;
        this.dayOfWeek = dayOfWeek;
        this.monthOfYear = monthOfYear;
    }

    public Long getId() {
        return id;
    }

    public AppUser getUser() {
        return user;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public int getDayOfMonth() {
        return dayOfMonth;
    }

    public void setDayOfMonth(int dayOfMonth) {
        this.dayOfMonth = dayOfMonth;
    }

    public FixedCostFrequency getFrequency() {
        return frequency;
    }

    public void setFrequency(FixedCostFrequency frequency) {
        this.frequency = frequency;
    }

    public Integer getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(Integer dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public Integer getMonthOfYear() {
        return monthOfYear;
    }

    public void setMonthOfYear(Integer monthOfYear) {
        this.monthOfYear = monthOfYear;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
