package CalenderApp.demo.service.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class NorwayHolidays {

    private NorwayHolidays() {
    }

    record Holiday(String code, LocalDate date, String title) {
    }

    static List<Holiday> forYear(int year) {
        List<Holiday> out = new ArrayList<>();

        // Fixed dates
        out.add(new Holiday("NEW_YEAR", LocalDate.of(year, 1, 1), "Helligdag: 1. nyttårsdag"));
        out.add(new Holiday("LABOUR_DAY", LocalDate.of(year, 5, 1), "Helligdag: 1. mai"));
        out.add(new Holiday("LIBERATION_DAY", LocalDate.of(year, 5, 8), "Merkedag: Frigjøringsdagen"));
        out.add(new Holiday("CONSTITUTION_DAY", LocalDate.of(year, 5, 17), "Helligdag: 17. mai"));
        out.add(new Holiday("CHRISTMAS_EVE", LocalDate.of(year, 12, 24), "Merkedag: Julaften"));
        out.add(new Holiday("CHRISTMAS_DAY", LocalDate.of(year, 12, 25), "Helligdag: 1. juledag"));
        out.add(new Holiday("BOXING_DAY", LocalDate.of(year, 12, 26), "Helligdag: 2. juledag"));
        out.add(new Holiday("NEW_YEARS_EVE", LocalDate.of(year, 12, 31), "Merkedag: Nyttårsaften"));

        // Movable (based on Easter)
        LocalDate easter = easterSunday(year);
        out.add(new Holiday("PALM_SUNDAY", easter.minusDays(7), "Helligdag: Palmesøndag"));
        out.add(new Holiday("MAUNDY_THURSDAY", easter.minusDays(3), "Helligdag: Skjærtorsdag"));
        out.add(new Holiday("GOOD_FRIDAY", easter.minusDays(2), "Helligdag: Langfredag"));
        out.add(new Holiday("EASTER_SUNDAY", easter, "Helligdag: 1. påskedag"));
        out.add(new Holiday("EASTER_MONDAY", easter.plusDays(1), "Helligdag: 2. påskedag"));
        out.add(new Holiday("ASCENSION_DAY", easter.plusDays(39), "Helligdag: Kristi himmelfartsdag"));
        out.add(new Holiday("PENTECOST", easter.plusDays(49), "Helligdag: 1. pinsedag"));
        out.add(new Holiday("PENTECOST_MONDAY", easter.plusDays(50), "Helligdag: 2. pinsedag"));

        out.sort(Comparator.comparing(Holiday::date).thenComparing(Holiday::code));
        return out;
    }

    // Anonymous Gregorian algorithm (Meeus/Jones/Butcher)
    static LocalDate easterSunday(int year) {
        if (year < -999_999_999 || year > 999_999_999) throw new IllegalArgumentException("Invalid year");

        int a = year % 19;
        int b = year / 100;
        int c = year % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int month = (h + l - 7 * m + 114) / 31;
        int day = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(year, month, day);
    }
}
