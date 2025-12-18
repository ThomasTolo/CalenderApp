package CalenderApp.demo.service;

import CalenderApp.demo.model.AppUser;
import CalenderApp.demo.service.command.CreateCalendarItemCommand;
import CalenderApp.demo.service.command.UpdateCalendarItemCommand;
import CalenderApp.demo.service.view.CalendarItemView;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public interface CalendarService {
    CalendarItemView create(AppUser user, CreateCalendarItemCommand command);

    CalendarItemView update(AppUser user, Long id, UpdateCalendarItemCommand command);

    void delete(AppUser user, Long id);

    List<CalendarItemView> listDay(AppUser user, LocalDate date);

    List<CalendarItemView> listMonth(AppUser user, YearMonth month);
}
