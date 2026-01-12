package CalenderApp.demo.repository;

import CalenderApp.demo.model.AppUser;
import CalenderApp.demo.model.BirthdaySubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BirthdaySubscriptionRepository extends JpaRepository<BirthdaySubscription, Long> {
    List<BirthdaySubscription> findByUserAndActiveTrue(AppUser user);

    Optional<BirthdaySubscription> findFirstByUserAndTitleAndMonthAndDayOfMonthOrderByCreatedAtAsc(
            AppUser user,
            String title,
            int month,
            int dayOfMonth
    );

    Optional<BirthdaySubscription> findByIdAndUser(Long id, AppUser user);
}
