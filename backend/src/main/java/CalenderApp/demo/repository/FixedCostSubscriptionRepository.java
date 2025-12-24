package CalenderApp.demo.repository;

import CalenderApp.demo.model.AppUser;
import CalenderApp.demo.model.FixedCostSubscription;
import CalenderApp.demo.model.FixedCostFrequency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface FixedCostSubscriptionRepository extends JpaRepository<FixedCostSubscription, Long> {
    List<FixedCostSubscription> findByUserAndActiveTrue(AppUser user);

    Optional<FixedCostSubscription> findFirstByUserAndTitleAndAmountAndDayOfMonthOrderByCreatedAtAsc(
            AppUser user,
            String title,
            BigDecimal amount,
            int dayOfMonth
    );

        @Query("""
            select s from FixedCostSubscription s
            where s.user = :user
              and s.title = :title
              and s.amount = :amount
              and s.frequency = :frequency
              and s.dayOfMonth = :dayOfMonth
              and ((:dayOfWeek is null and s.dayOfWeek is null) or s.dayOfWeek = :dayOfWeek)
              and ((:monthOfYear is null and s.monthOfYear is null) or s.monthOfYear = :monthOfYear)
            order by s.createdAt asc
            """)
        List<FixedCostSubscription> findBySignature(
            @Param("user") AppUser user,
            @Param("title") String title,
            @Param("amount") BigDecimal amount,
            @Param("frequency") FixedCostFrequency frequency,
            @Param("dayOfMonth") int dayOfMonth,
            @Param("dayOfWeek") Integer dayOfWeek,
            @Param("monthOfYear") Integer monthOfYear,
            Pageable pageable
        );

    Optional<FixedCostSubscription> findByIdAndUser(Long id, AppUser user);
}
