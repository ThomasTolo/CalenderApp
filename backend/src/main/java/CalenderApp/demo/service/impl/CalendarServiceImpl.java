package main.java.CalenderApp.demo.service.impl;

import CalenderApp.demo.model.AppUser;
import CalenderApp.demo.model.CalendarItem;
import CalenderApp.demo.model.Notification;
import CalenderApp.demo.model.NotificationType;
import CalenderApp.demo.repository.CalendarItemRepository;
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

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
public class CalendarServiceImpl implements CalendarService {

    private final CalendarItemRepository itemRepository;
    private final CalendarMonthCache monthCache;
    private final EventPublisher eventPublisher;
    private final NotificationService notificationService;

    public CalendarServiceImpl(
            CalendarItemRepository itemRepository,
            CalendarMonthCache monthCache,
            EventPublisher eventPublisher,
            NotificationService notificationService
    ) {
        this.itemRepository = itemRepository;
        this.monthCache = monthCache;
        this.eventPublisher = eventPublisher;
        this.notificationService = notificationService;
    }

    @Override
    public CalendarItemView create(AppUser user, CreateCalendarItemCommand command) {
        validateTimes(command.startTime(), command.endTime());

        CalendarItem item = new CalendarItem(user, command.date(), command.type(), command.title());
        item.setStartTime(command.startTime());
        item.setEndTime(command.endTime());
        item.setImportance(command.importance());
        item.setLog(command.log());

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
    public CalendarItemView update(AppUser user, Long id, UpdateCalendarItemCommand command) {
        validateTimes(command.startTime(), command.endTime());

        CalendarItem existing = itemRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new NotFoundException("Calendar item not found"));

        YearMonth oldMonth = YearMonth.from(existing.getDate());

        existing.setDate(command.date());
        existing.setStartTime(command.startTime());
        existing.setEndTime(command.endTime());
        existing.setType(command.type());
        existing.setImportance(command.importance());
        existing.setTitle(command.title());
        existing.setLog(command.log());

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
    public void delete(AppUser user, Long id) {
        CalendarItem existing = itemRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new NotFoundException("Calendar item not found"));

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
    public List<CalendarItemView> listDay(AppUser user, LocalDate date) {
        return itemRepository.findByUserAndDateOrderByStartTimeAsc(user, date)
                .stream()
                .map(CalendarServiceImpl::toView)
                .toList();
    }

    @Override
    public List<CalendarItemView> listMonth(AppUser user, YearMonth month) {
        return monthCache.get(user.getId(), month)
                .orElseGet(() -> {
                    LocalDate start = month.atDay(1);
                    LocalDate end = month.atEndOfMonth();
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
        return new CalendarItemView(
                item.getId(),
                item.getDate(),
                item.getStartTime(),
                item.getEndTime(),
                item.getType(),
                item.getImportance(),
                item.getTitle(),
                item.getLog(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }
}
