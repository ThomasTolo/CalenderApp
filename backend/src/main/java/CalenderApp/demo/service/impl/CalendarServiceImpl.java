package CalenderApp.demo.service.impl;

import CalenderApp.demo.model.AppUser;
import CalenderApp.demo.model.CalendarItem;
import CalenderApp.demo.model.CalendarItemType;
import CalenderApp.demo.model.FixedCostFrequency;
import CalenderApp.demo.model.FixedCostSubscription;
import CalenderApp.demo.model.Notification;
import CalenderApp.demo.model.NotificationType;
import CalenderApp.demo.model.SchoolItemKind;
import CalenderApp.demo.repository.CalendarItemRepository;
import CalenderApp.demo.repository.FixedCostSubscriptionRepository;
import CalenderApp.demo.service.CalendarMonthCache;
import CalenderApp.demo.service.CalendarService;
import CalenderApp.demo.service.EventPublisher;
import CalenderApp.demo.service.NotificationService;
import CalenderApp.demo.service.command.CreateCalendarItemCommand;
import CalenderApp.demo.service.command.UpdateCalendarItemCommand;
import CalenderApp.demo.service.exception.BadRequestException;
import CalenderApp.demo.service.exception.NotFoundException;
import CalenderApp.demo.service.view.CalendarItemView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
public class CalendarServiceImpl implements CalendarService {

    private final CalendarItemRepository itemRepository;
    private final CalendarMonthCache monthCache;
    private final EventPublisher eventPublisher;
    private final NotificationService notificationService;
    private final FixedCostSubscriptionRepository fixedCostSubscriptionRepository;

    public CalendarServiceImpl(
            CalendarItemRepository itemRepository,
            CalendarMonthCache monthCache,
            EventPublisher eventPublisher,
            NotificationService notificationService,
            FixedCostSubscriptionRepository fixedCostSubscriptionRepository
    ) {
        this.itemRepository = itemRepository;
        this.monthCache = monthCache;
        this.eventPublisher = eventPublisher;
        this.notificationService = notificationService;
        this.fixedCostSubscriptionRepository = fixedCostSubscriptionRepository;
    }

    @Override
    @Transactional
    public CalendarItemView create(AppUser user, CreateCalendarItemCommand command) {
        validateTimes(command.startTime(), command.endTime());

        if (command.type() == CalendarItemType.FIXED_COST && command.amount() == null) {
            throw new BadRequestException("amount is required for FIXED_COST");
        }

        CalendarItem item = new CalendarItem(user, command.date(), command.type(), command.title());
        item.setStartTime(command.startTime());
        item.setEndTime(command.endTime());
        item.setImportance(command.importance());
        item.setLog(command.log());
        item.setDone(command.done() != null && command.done());
        item.setAmount(command.amount());
        item.setSchoolKind(defaultSchoolKind(command.type(), command.schoolKind()));

        if (command.type() == CalendarItemType.FIXED_COST) {
            FixedCostSubscription sub = createOrUpdateSubscription(user, null, command);
            item.setFixedCostSubscription(sub);
        }

        CalendarItem saved = itemRepository.save(item);
        evictMonth(user.getId(), YearMonth.from(saved.getDate()));

        eventPublisher.calendarItemEvent(NotificationType.ITEM_CREATED, saved);
        notificationService.create(new Notification(
                user,
                NotificationType.ITEM_CREATED,
                saved.getImportance(),
                "Created " + saved.getType() + " on " + saved.getDate() + ": " + saved.getTitle(),
                saved.getId()
        ));

        return toView(saved);
    }

    @Override
    @Transactional
    public CalendarItemView update(AppUser user, Long id, UpdateCalendarItemCommand command) {
        validateTimes(command.startTime(), command.endTime());

        if (command.type() == CalendarItemType.FIXED_COST && command.amount() == null) {
            throw new BadRequestException("amount is required for FIXED_COST");
        }

        CalendarItem existing = itemRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new NotFoundException("Calendar item not found"));

        if (isSystemHoliday(existing)) {
            throw new BadRequestException("Holiday items cannot be edited");
        }

        YearMonth oldMonth = YearMonth.from(existing.getDate());

        existing.setDate(command.date());
        existing.setStartTime(command.startTime());
        existing.setEndTime(command.endTime());
        existing.setType(command.type());
        existing.setImportance(command.importance());
        existing.setTitle(command.title());
        existing.setLog(command.log());
        existing.setDone(command.done() != null ? command.done() : existing.isDone());
        existing.setAmount(command.amount());
        existing.setSchoolKind(defaultSchoolKind(command.type(), command.schoolKind()));

        if (command.type() == CalendarItemType.FIXED_COST) {
            FixedCostSubscription sub = createOrUpdateSubscription(user, existing.getFixedCostSubscription(), command);
            existing.setFixedCostSubscription(sub);
            syncFutureFixedCostOccurrences(user, sub);
        } else {
            existing.setFixedCostSubscription(null);
        }

        CalendarItem saved = itemRepository.save(existing);

        evictMonth(user.getId(), oldMonth);
        evictMonth(user.getId(), YearMonth.from(saved.getDate()));

        eventPublisher.calendarItemEvent(NotificationType.ITEM_UPDATED, saved);
        notificationService.create(new Notification(
                user,
                NotificationType.ITEM_UPDATED,
                saved.getImportance(),
                "Updated " + saved.getType() + " on " + saved.getDate() + ": " + saved.getTitle(),
                saved.getId()
        ));

        return toView(saved);
    }

    @Override
    @Transactional
    public void delete(AppUser user, Long id) {
        CalendarItem existing = itemRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new NotFoundException("Calendar item not found"));

        if (isSystemHoliday(existing)) {
            throw new BadRequestException("Holiday items cannot be deleted");
        }

        if (existing.getType() == CalendarItemType.FIXED_COST && existing.getFixedCostSubscription() != null) {
            FixedCostSubscription sub = java.util.Objects.requireNonNull(existing.getFixedCostSubscription());
            LocalDate today = LocalDate.now();
            LocalDate end = LocalDate.of(3000, 1, 1);
            List<CalendarItem> future = java.util.Objects.requireNonNull(
                itemRepository.findByUserAndFixedCostSubscriptionAndDateBetween(user, sub, today, end)
            );
            itemRepository.deleteAll(future);
            sub.setActive(false);
            fixedCostSubscriptionRepository.save(sub);

            notificationService.create(new Notification(
                user,
                NotificationType.ITEM_DELETED,
                existing.getImportance(),
                "Unsubscribed FIXED_COST: " + sub.getTitle(),
                existing.getId()
            ));
            evictMonth(user.getId(), YearMonth.from(existing.getDate()));
            return;
        }

        YearMonth month = YearMonth.from(existing.getDate());
        itemRepository.delete(existing);
        evictMonth(user.getId(), month);

        eventPublisher.calendarItemEvent(NotificationType.ITEM_DELETED, existing);
        notificationService.create(new Notification(
                user,
                NotificationType.ITEM_DELETED,
                existing.getImportance(),
                "Deleted " + existing.getType() + " on " + existing.getDate() + ": " + existing.getTitle(),
                existing.getId()
        ));
    }

    @Override
    public List<CalendarItemView> listDay(AppUser user, LocalDate date, CalenderApp.demo.model.CalendarItemType type) {
        if (type == null || type == CalendarItemType.FIXED_COST) {
            ensureFixedCostOccurrences(user, YearMonth.from(date));
        }
        if (type == null || type == CalendarItemType.OTHER) {
            ensureNorwayHolidays(user, YearMonth.from(date));
        }
        if (type == null) {
            return itemRepository.findByUserAndDateOrderByStartTimeAsc(user, date)
                    .stream()
                    .map(CalendarServiceImpl::toView)
                    .toList();
        }
        return itemRepository.findByUserAndDateAndTypeOrderByStartTimeAsc(user, date, type)
                .stream()
                .map(CalendarServiceImpl::toView)
                .toList();
    }

    @Override
    public List<CalendarItemView> listMonth(AppUser user, YearMonth month, CalenderApp.demo.model.CalendarItemType type) {
        if (type == null || type == CalendarItemType.FIXED_COST) {
            ensureFixedCostOccurrences(user, month);
        }
        if (type == null || type == CalendarItemType.OTHER) {
            ensureNorwayHolidays(user, month);
        }
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();
        if (type != null) {
            return itemRepository.findByUserAndDateBetweenAndTypeOrderByDateAscStartTimeAsc(user, start, end, type)
                    .stream()
                    .map(CalendarServiceImpl::toView)
                    .toList();
        }

        return monthCache.get(user.getId(), month)
                .orElseGet(() -> {
                    List<CalendarItemView> items = itemRepository.findByUserAndDateBetweenOrderByDateAscStartTimeAsc(user, start, end)
                            .stream()
                            .map(CalendarServiceImpl::toView)
                            .toList();
                    monthCache.put(user.getId(), month, items);
                    return items;
                });
    }

    private void evictMonth(Long userId, YearMonth month) {
        monthCache.evict(userId, month);
    }

    private static void validateTimes(java.time.LocalTime start, java.time.LocalTime end) {
        if (start != null && end != null && end.isBefore(start)) {
            throw new BadRequestException("endTime must be after startTime");
        }
    }

    private static CalendarItemView toView(CalendarItem item) {
        FixedCostFrequency freq = null;
        if (item.getType() == CalendarItemType.FIXED_COST) {
            FixedCostSubscription sub = item.getFixedCostSubscription();
            freq = sub != null && sub.getFrequency() != null ? sub.getFrequency() : FixedCostFrequency.MONTHLY;
        }
        return new CalendarItemView(
                item.getId(),
                item.getDate(),
                item.getStartTime(),
                item.getEndTime(),
                item.getType(),
                item.getImportance(),
                item.getTitle(),
                item.getLog(),
                item.isDone(),
                item.getAmount(),
                item.getSchoolKind(),
                freq,
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }

    private void ensureFixedCostOccurrences(AppUser user, YearMonth month) {
        boolean createdAny = false;

        for (FixedCostSubscription sub : fixedCostSubscriptionRepository.findByUserAndActiveTrue(user)) {
            FixedCostFrequency freq = effectiveFrequency(sub);

            if (freq == FixedCostFrequency.YEARLY) {
                Integer mo = sub.getMonthOfYear();
                if (mo == null || mo != month.getMonthValue()) {
                    continue;
                }
            }

            if (freq == FixedCostFrequency.WEEKLY) {
                int dowVal = sub.getDayOfWeek() != null ? sub.getDayOfWeek() : DayOfWeek.MONDAY.getValue();
                DayOfWeek dow = DayOfWeek.of(dowVal);
                LocalDate cursor = month.atDay(1).with(TemporalAdjusters.nextOrSame(dow));
                while (YearMonth.from(cursor).equals(month)) {
                    if (!itemRepository.existsByUserAndFixedCostSubscriptionAndDate(user, sub, cursor)) {
                        CalendarItem item = new CalendarItem(user, cursor, CalendarItemType.FIXED_COST, sub.getTitle());
                        item.setAmount(sub.getAmount());
                        item.setImportance(CalenderApp.demo.model.ImportanceLevel.MEDIUM);
                        item.setFixedCostSubscription(sub);
                        itemRepository.save(item);
                        createdAny = true;
                    }
                    cursor = cursor.plusWeeks(1);
                }
                continue;
            }

            int dom = sub.getDayOfMonth();
            int capped = Math.min(dom, month.lengthOfMonth());
            LocalDate date = month.atDay(capped);
            if (itemRepository.existsByUserAndFixedCostSubscriptionAndDate(user, sub, date)) {
                continue;
            }

            CalendarItem item = new CalendarItem(user, date, CalendarItemType.FIXED_COST, sub.getTitle());
            item.setAmount(sub.getAmount());
            item.setImportance(CalenderApp.demo.model.ImportanceLevel.MEDIUM);
            item.setFixedCostSubscription(sub);
            itemRepository.save(item);
            createdAny = true;
        }

        if (createdAny) {
            evictMonth(user.getId(), month);
        }
    }

    private void ensureNorwayHolidays(AppUser user, YearMonth month) {
        boolean createdAny = false;
        for (NorwayHolidays.Holiday h : NorwayHolidays.forYear(month.getYear())) {
            if (!YearMonth.from(h.date()).equals(month)) {
                continue;
            }
            if (itemRepository.existsByUserAndDateAndTypeAndTitle(user, h.date(), CalendarItemType.OTHER, h.title())) {
                continue;
            }
            CalendarItem item = new CalendarItem(user, h.date(), CalendarItemType.OTHER, h.title());
            item.setImportance(CalenderApp.demo.model.ImportanceLevel.LOW);
            itemRepository.save(item);
            createdAny = true;
        }
        if (createdAny) {
            evictMonth(user.getId(), month);
        }
    }

    private static boolean isSystemHoliday(CalendarItem item) {
        if (item.getType() != CalendarItemType.OTHER) {
            return false;
        }
        String title = item.getTitle();
        if (title == null) {
            return false;
        }
        return title.startsWith("Merkedag:") || title.startsWith("Helligdag:");
    }

    private FixedCostSubscription createOrUpdateSubscription(AppUser user, FixedCostSubscription existing, CreateCalendarItemCommand command) {
        return createOrUpdateSubscription(
                user,
                existing,
                command.title(),
                command.amount(),
                command.date(),
                command.fixedCostFrequency()
        );
    }

    private FixedCostSubscription createOrUpdateSubscription(AppUser user, FixedCostSubscription existing, UpdateCalendarItemCommand command) {
        return createOrUpdateSubscription(
                user,
                existing,
                command.title(),
                command.amount(),
                command.date(),
                command.fixedCostFrequency()
        );
    }

    private FixedCostSubscription createOrUpdateSubscription(
            AppUser user,
            FixedCostSubscription existing,
            String title,
            java.math.BigDecimal amount,
            LocalDate date,
            FixedCostFrequency requestedFrequency
    ) {
        FixedCostFrequency frequency = requestedFrequency;
        if (frequency == null) {
            frequency = existing != null && existing.getFrequency() != null
                    ? existing.getFrequency()
                    : FixedCostFrequency.MONTHLY;
        }

        int dayOfMonth = 1;
        Integer dayOfWeek = null;
        Integer monthOfYear = null;

        if (frequency == FixedCostFrequency.WEEKLY) {
            dayOfWeek = date != null ? date.getDayOfWeek().getValue() : DayOfWeek.MONDAY.getValue();
        } else if (frequency == FixedCostFrequency.YEARLY) {
            monthOfYear = date != null ? date.getMonthValue() : 1;
            dayOfMonth = date != null ? date.getDayOfMonth() : 1;
        } else {
            dayOfMonth = date != null ? date.getDayOfMonth() : 1;
        }

        // For WEEKLY, keep dayOfMonth stable to avoid signature drift.
        if (frequency == FixedCostFrequency.WEEKLY) {
            dayOfMonth = 1;
        }

        if (existing == null) {
            List<FixedCostSubscription> found = fixedCostSubscriptionRepository.findBySignature(
                    user,
                    title,
                    amount,
                    frequency,
                    dayOfMonth,
                    dayOfWeek,
                    monthOfYear,
                    PageRequest.of(0, 1)
            );
            FixedCostSubscription sub = found.isEmpty()
                    ? new FixedCostSubscription(user, title, amount, frequency, dayOfMonth, dayOfWeek, monthOfYear)
                    : found.get(0);
            sub.setActive(true);
            if (sub.getFrequency() == null) sub.setFrequency(frequency);
            if (sub.getDayOfWeek() == null && dayOfWeek != null) sub.setDayOfWeek(dayOfWeek);
            if (sub.getMonthOfYear() == null && monthOfYear != null) sub.setMonthOfYear(monthOfYear);
            return fixedCostSubscriptionRepository.save(sub);
        }

        existing.setTitle(title);
        if (amount != null) existing.setAmount(amount);
        existing.setFrequency(frequency);
        existing.setDayOfMonth(dayOfMonth);
        existing.setDayOfWeek(dayOfWeek);
        existing.setMonthOfYear(monthOfYear);
        existing.setActive(true);
        return fixedCostSubscriptionRepository.save(existing);
    }

    private static FixedCostFrequency effectiveFrequency(FixedCostSubscription sub) {
        return sub.getFrequency() != null ? sub.getFrequency() : FixedCostFrequency.MONTHLY;
    }

    private void syncFutureFixedCostOccurrences(AppUser user, FixedCostSubscription sub) {
        LocalDate today = LocalDate.now();
        LocalDate end = LocalDate.of(3000, 1, 1);
        List<CalendarItem> future = itemRepository.findByUserAndFixedCostSubscriptionAndDateBetween(user, sub, today, end);
        if (future.isEmpty()) {
            return;
        }

        for (CalendarItem item : future) {
            item.setTitle(sub.getTitle());
            item.setAmount(sub.getAmount());
        }
        itemRepository.saveAll(future);
    }

    private static SchoolItemKind defaultSchoolKind(CalendarItemType type, SchoolItemKind kind) {
        if (type != CalendarItemType.SCHOOL) {
            return null;
        }
        return kind != null ? kind : SchoolItemKind.LECTURE;
    }
}
