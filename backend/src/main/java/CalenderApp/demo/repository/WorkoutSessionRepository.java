package CalenderApp.demo.repository;

import CalenderApp.demo.model.AppUser;
import CalenderApp.demo.model.WorkoutSession;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkoutSessionRepository extends JpaRepository<WorkoutSession, Long> {
    @EntityGraph(attributePaths = {"entries", "entries.exercise"})
    Optional<WorkoutSession> findByUserAndCalendarItem_Id(AppUser user, Long calendarItemId);
}
