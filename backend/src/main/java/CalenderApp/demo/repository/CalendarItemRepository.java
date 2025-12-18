package main.java.CalenderApp.demo.repository;

import CalenderApp.demo.model.AppUser;
import CalenderApp.demo.model.CalendarItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CalendarItemRepository extends JpaRepository<CalendarItem, Long> {
    List<CalendarItem> findByUserAndDate(AppUser user, LocalDate date);

    List<CalendarItem> findByUserAndDateOrderByStartTimeAsc(AppUser user, LocalDate date);

    List<CalendarItem> findByUserAndDateBetweenOrderByDateAscStartTimeAsc(AppUser user, LocalDate start, LocalDate end);

    Optional<CalendarItem> findByIdAndUser(Long id, AppUser user);

    List<CalendarItem> findByDateAndNotifiedFalseAndStartTimeIsNotNull(LocalDate date);
}
