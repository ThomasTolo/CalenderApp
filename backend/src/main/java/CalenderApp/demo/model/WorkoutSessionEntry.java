package CalenderApp.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "workout_session_entries")
public class WorkoutSessionEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private WorkoutSession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    @Column(nullable = false)
    private int sets;

    @Column(nullable = false)
    private int reps;

    @Column(precision = 8, scale = 2)
    private BigDecimal weight;

    @Column(nullable = false)
    private int position;

    protected WorkoutSessionEntry() {
    }

    public WorkoutSessionEntry(WorkoutSession session, Exercise exercise, int sets, int reps, BigDecimal weight, int position) {
        this.session = session;
        this.exercise = exercise;
        this.sets = sets;
        this.reps = reps;
        this.weight = weight;
        this.position = position;
    }

    public Long getId() {
        return id;
    }

    public WorkoutSession getSession() {
        return session;
    }

    public Exercise getExercise() {
        return exercise;
    }

    public int getSets() {
        return sets;
    }

    public void setSets(int sets) {
        this.sets = sets;
    }

    public int getReps() {
        return reps;
    }

    public void setReps(int reps) {
        this.reps = reps;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}
