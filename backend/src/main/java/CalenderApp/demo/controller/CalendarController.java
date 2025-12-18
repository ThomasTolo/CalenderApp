package CalenderApp.demo.controller;

import CalenderApp.demo.controller.dto.CalendarItemCreateRequest;
import CalenderApp.demo.controller.dto.CalendarItemResponse;
import CalenderApp.demo.controller.dto.CalendarItemUpdateRequest;
import CalenderApp.demo.controller.dto.CalendarMonthResponse;
import CalenderApp.demo.model.AppUser;
import CalenderApp.demo.service.CalendarService;
import CalenderApp.demo.service.CurrentUserService;
import CalenderApp.demo.service.command.CreateCalendarItemCommand;
import CalenderApp.demo.service.command.UpdateCalendarItemCommand;
import CalenderApp.demo.service.view.CalendarItemView;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/calendar")
public class CalendarController {

    private final CalendarService calendarService;
    private final CurrentUserService currentUserService;

    public CalendarController(CalendarService calendarService, CurrentUserService currentUserService) {
        this.calendarService = calendarService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/day")
    public List<CalendarItemResponse> getDay(@RequestParam LocalDate date, Principal principal) {
        AppUser user = currentUserService.requireByUsername(principal.getName());
        return calendarService.listDay(user, date).stream().map(CalendarController::toResponse).toList();
    }

    @GetMapping("/month")
    public CalendarMonthResponse getMonth(@RequestParam int year, @RequestParam int month, Principal principal) {
        AppUser user = currentUserService.requireByUsername(principal.getName());
        YearMonth ym = YearMonth.of(year, month);
        List<CalendarItemResponse> items = calendarService.listMonth(user, ym).stream().map(CalendarController::toResponse).toList();
        return new CalendarMonthResponse(year, month, items);
    }

    @PostMapping("/items")
    public CalendarItemResponse create(@Valid @RequestBody CalendarItemCreateRequest request, Principal principal) {
        AppUser user = currentUserService.requireByUsername(principal.getName());
        CalendarItemView created = calendarService.create(user, new CreateCalendarItemCommand(
                request.date(),
                request.startTime(),
                request.endTime(),
                request.type(),
                request.importance(),
                request.title(),
                request.log()
        ));
        return toResponse(created);
    }

    @PutMapping("/items/{id}")
    public CalendarItemResponse update(@PathVariable Long id, @Valid @RequestBody CalendarItemUpdateRequest request, Principal principal) {
        AppUser user = currentUserService.requireByUsername(principal.getName());
        CalendarItemView updated = calendarService.update(user, id, new UpdateCalendarItemCommand(
                request.date(),
                request.startTime(),
                request.endTime(),
                request.type(),
                request.importance(),
                request.title(),
                request.log()
        ));
        return toResponse(updated);
    }

    @DeleteMapping("/items/{id}")
    public void delete(@PathVariable Long id, Principal principal) {
        AppUser user = currentUserService.requireByUsername(principal.getName());
        calendarService.delete(user, id);
    }

    private static CalendarItemResponse toResponse(CalendarItemView view) {
        return new CalendarItemResponse(
                view.id(),
                view.date(),
                view.startTime(),
                view.endTime(),
                view.type(),
                view.importance(),
                view.title(),
                view.log(),
                view.createdAt(),
                view.updatedAt()
        );
    }
}
