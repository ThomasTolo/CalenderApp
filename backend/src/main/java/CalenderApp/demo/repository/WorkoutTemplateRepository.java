package CalenderApp.demo.repository;

import CalenderApp.demo.model.AppUser;
import CalenderApp.demo.model.WorkoutTemplate;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkoutTemplateRepository extends JpaRepository<WorkoutTemplate, Long> {
    @EntityGraph(attributePaths = {"entries", "entries.exercise"})
    List<WorkoutTemplate> findByUserOrderByUpdatedAtDesc(AppUser user);

    @EntityGraph(attributePaths = {"entries", "entries.exercise"})
    Optional<WorkoutTemplate> findByIdAndUser(Long id, AppUser user);
}
