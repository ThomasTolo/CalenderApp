package CalenderApp.demo.config.db;

import CalenderApp.demo.model.CalendarItem;
import CalenderApp.demo.model.CalendarItemType;
import CalenderApp.demo.model.FixedCostFrequency;
import CalenderApp.demo.model.FixedCostSubscription;
import CalenderApp.demo.repository.CalendarItemRepository;
import CalenderApp.demo.repository.FixedCostSubscriptionRepository;
import CalenderApp.demo.service.CalendarMonthCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
public class CalendarSystemItemDeduplicator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CalendarSystemItemDeduplicator.class);

    private final CalendarItemRepository itemRepository;
    private final FixedCostSubscriptionRepository fixedCostSubscriptionRepository;
    private final CalendarMonthCache monthCache;

    public CalendarSystemItemDeduplicator(
            CalendarItemRepository itemRepository,
            FixedCostSubscriptionRepository fixedCostSubscriptionRepository,
            CalendarMonthCache monthCache
    ) {
        this.itemRepository = itemRepository;
        this.fixedCostSubscriptionRepository = fixedCostSubscriptionRepository;
        this.monthCache = monthCache;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        try {
            Set<EvictKey> monthsToEvict = new HashSet<>();

            int holidaysRemoved = dedupeHolidays(monthsToEvict);
            int fixedCostItemsRemoved = dedupeFixedCostItems(monthsToEvict);
            int subsDeactivated = dedupeFixedCostSubscriptions(monthsToEvict);

            for (EvictKey k : monthsToEvict) {
                monthCache.evict(k.userId(), k.month());
            }

            if (holidaysRemoved > 0 || fixedCostItemsRemoved > 0 || subsDeactivated > 0) {
                log.info(
                        "Dedup complete: holidaysRemoved={}, fixedCostItemsRemoved={}, subscriptionsDeactivated={}, monthsEvicted={}",
                        holidaysRemoved,
                        fixedCostItemsRemoved,
                        subsDeactivated,
                        monthsToEvict.size()
                );
            }
        } catch (Exception e) {
            // Best-effort cleanup; never block startup.
            log.debug("Dedup skipped due to error: {}", e.getMessage());
        }
    }

    private int dedupeHolidays(Set<EvictKey> monthsToEvict) {
        List<CalendarItem> holidays = new ArrayList<>();
        holidays.addAll(itemRepository.findByTypeAndTitleStartingWith(CalendarItemType.OTHER, "Merkedag:"));
        holidays.addAll(itemRepository.findByTypeAndTitleStartingWith(CalendarItemType.OTHER, "Helligdag:"));

        holidays.sort(Comparator.comparing(CalendarItem::getId));

        Map<String, Long> seen = new HashMap<>();
        List<Long> toDelete = new ArrayList<>();

        for (CalendarItem it : holidays) {
            Long userId = it.getUser() != null ? it.getUser().getId() : null;
            if (userId == null || it.getDate() == null || it.getTitle() == null) continue;

            String key = userId + "|" + it.getDate() + "|" + it.getTitle();
            if (!seen.containsKey(key)) {
                seen.put(key, it.getId());
                continue;
            }

            toDelete.add(it.getId());
            monthsToEvict.add(new EvictKey(userId, YearMonth.from(it.getDate())));
        }

        if (!toDelete.isEmpty()) {
            itemRepository.deleteAllByIdInBatch(toDelete);
        }

        return toDelete.size();
    }

    private int dedupeFixedCostSubscriptions(Set<EvictKey> monthsToEvict) {
        List<FixedCostSubscription> subs = fixedCostSubscriptionRepository.findAll();
        subs.sort(Comparator
                .comparing(FixedCostSubscription::getCreatedAt)
                .thenComparing(FixedCostSubscription::getId)
        );

        Map<String, FixedCostSubscription> keep = new HashMap<>();
        int deactivated = 0;

        LocalDate start = LocalDate.of(1900, 1, 1);
        LocalDate end = LocalDate.of(3000, 1, 1);

        for (FixedCostSubscription sub : subs) {
            if (sub.getUser() == null || sub.getUser().getId() == null) continue;
            if (sub.getTitle() == null || sub.getAmount() == null) continue;

            FixedCostFrequency freq = sub.getFrequency() != null ? sub.getFrequency() : FixedCostFrequency.MONTHLY;
            String key = sub.getUser().getId()
                    + "|" + sub.getTitle()
                    + "|" + sub.getAmount()
                    + "|" + freq
                    + "|" + sub.getDayOfMonth()
                    + "|" + sub.getDayOfWeek()
                    + "|" + sub.getMonthOfYear();
            FixedCostSubscription existing = keep.get(key);
            if (existing == null) {
                keep.put(key, sub);
                continue;
            }

            if (sub.isActive()) {
                sub.setActive(false);
                fixedCostSubscriptionRepository.save(sub);
                deactivated++;
            }

            List<CalendarItem> items = itemRepository.findByUserAndFixedCostSubscriptionAndDateBetween(sub.getUser(), sub, start, end);
            if (!items.isEmpty()) {
                for (CalendarItem it : items) {
                    if (it.getDate() != null) {
                        monthsToEvict.add(new EvictKey(sub.getUser().getId(), YearMonth.from(it.getDate())));
                    }
                }
                itemRepository.deleteAllInBatch(items);
            }
        }

        return deactivated;
    }

    private int dedupeFixedCostItems(Set<EvictKey> monthsToEvict) {
        int removed = 0;
        LocalDate start = LocalDate.of(1900, 1, 1);
        LocalDate end = LocalDate.of(3000, 1, 1);

        for (FixedCostSubscription sub : fixedCostSubscriptionRepository.findAll()) {
            if (sub.getUser() == null || sub.getUser().getId() == null) continue;

            List<CalendarItem> items = itemRepository.findByUserAndFixedCostSubscriptionAndDateBetween(sub.getUser(), sub, start, end);
            if (items.size() <= 1) continue;

            items.sort(Comparator.comparing(CalendarItem::getId));

            Map<LocalDate, Long> seenDate = new HashMap<>();
            List<Long> toDelete = new ArrayList<>();

            for (CalendarItem it : items) {
                if (it.getType() != CalendarItemType.FIXED_COST) continue;
                LocalDate date = it.getDate();
                if (date == null) continue;

                if (!seenDate.containsKey(date)) {
                    seenDate.put(date, it.getId());
                    continue;
                }

                toDelete.add(it.getId());
                monthsToEvict.add(new EvictKey(sub.getUser().getId(), YearMonth.from(date)));
            }

            if (!toDelete.isEmpty()) {
                itemRepository.deleteAllByIdInBatch(toDelete);
                removed += toDelete.size();
            }
        }

        return removed;
    }

    private record EvictKey(Long userId, YearMonth month) {
        EvictKey {
            Objects.requireNonNull(userId);
            Objects.requireNonNull(month);
        }
    }
}
