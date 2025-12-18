package CalenderApp.demo.service;

import CalenderApp.demo.service.view.CalendarItemView;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface CalendarMonthCache {
    Optional<List<CalendarItemView>> get(Long userId, YearMonth month);

    void put(Long userId, YearMonth month, List<CalendarItemView> items);

    void evict(Long userId, YearMonth month);
}
