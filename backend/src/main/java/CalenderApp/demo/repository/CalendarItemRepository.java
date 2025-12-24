package CalenderApp.demo.repository;

import CalenderApp.demo.model.AppUser;
import CalenderApp.demo.model.CalendarItem;
import CalenderApp.demo.model.CalendarItemType;
import CalenderApp.demo.model.FixedCostSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CalendarItemRepository extends JpaRepository<CalendarItem, Long> {
    List<CalendarItem> findByUserAndDate(AppUser user, LocalDate date);

    @EntityGraph(attributePaths = {"fixedCostSubscription"})
    List<CalendarItem> findByUserAndDateOrderByStartTimeAsc(AppUser user, LocalDate date);

    @EntityGraph(attributePaths = {"fixedCostSubscription"})
    List<CalendarItem> findByUserAndDateAndTypeOrderByStartTimeAsc(AppUser user, LocalDate date, CalendarItemType type);

    @EntityGraph(attributePaths = {"fixedCostSubscription"})
    List<CalendarItem> findByUserAndDateBetweenOrderByDateAscStartTimeAsc(AppUser user, LocalDate start, LocalDate end);

    @EntityGraph(attributePaths = {"fixedCostSubscription"})
    List<CalendarItem> findByUserAndDateBetweenAndTypeOrderByDateAscStartTimeAsc(AppUser user, LocalDate start, LocalDate end, CalendarItemType type);

    Optional<CalendarItem> findByIdAndUser(Long id, AppUser user);

    Optional<CalendarItem> findTopByUserAndTypeAndDoneTrueOrderByDateDesc(AppUser user, CalendarItemType type);

    boolean existsByUserAndDateAndTypeAndTitle(AppUser user, LocalDate date, CalendarItemType type, String title);

    List<CalendarItem> findByTypeAndTitleStartingWith(CalendarItemType type, String titlePrefix);

    List<CalendarItem> findByDateAndNotifiedFalseAndStartTimeIsNotNull(LocalDate date);

    List<CalendarItem> findByDateBetweenAndNotifiedFalseAndStartTimeIsNotNull(LocalDate start, LocalDate end);

    List<CalendarItem> findByDateAndTypeAndNotifiedFalse(LocalDate date, CalendarItemType type);

    boolean existsByUserAndFixedCostSubscriptionAndDate(AppUser user, FixedCostSubscription sub, LocalDate date);

    List<CalendarItem> findByUserAndFixedCostSubscriptionAndDateBetween(AppUser user, FixedCostSubscription sub, LocalDate start, LocalDate end);
}
