package CalenderApp.demo.repository;

import CalenderApp.demo.model.AppUser;
import CalenderApp.demo.model.Exercise;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExerciseRepository extends JpaRepository<Exercise, Long> {
    List<Exercise> findByUserOrderByNameAsc(AppUser user);

    Optional<Exercise> findByIdAndUser(Long id, AppUser user);

    boolean existsByUserAndNameIgnoreCase(AppUser user, String name);
}
