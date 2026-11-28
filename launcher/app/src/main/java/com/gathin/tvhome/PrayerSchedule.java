package com.gathin.tvhome;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Locale;

/** Pure date/time helpers for the Malaysian prayer bar. */
final class PrayerSchedule {
    static final String ZONE = "SGR03";
    static final java.time.ZoneId MALAYSIA = java.time.ZoneId.of("Asia/Kuala_Lumpur");
    private static final DateTimeFormatter JAKIM_DATE = DateTimeFormatter
            .ofPattern("d-MMM-uuuu", Locale.ENGLISH).withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);

    static LocalDate parseDate(String value) {
        return LocalDate.parse(value, JAKIM_DATE);
    }

    static LocalTime parseTime(String value) {
        if (!value.matches("[0-9]{2}:[0-9]{2}(:[0-9]{2})?"))
            throw new IllegalArgumentException("Invalid prayer time");
        return LocalTime.parse(value);
    }

    static String display(LocalTime time) { return DISPLAY.format(time); }

    /** -1 after Isyak; the next prayer is tomorrow, not today's Subuh. */
    static int next(LocalTime[] times, LocalTime now) {
        for (int i = 0; i < times.length; i++)
            if (!now.isAfter(times[i])) return i;
        return -1;
    }

    static boolean ordered(LocalTime[] times) {
        if (times == null || times.length != 5) return false;
        for (int i = 0; i < times.length; i++)
            if (times[i] == null || (i > 0 && !times[i].isAfter(times[i - 1]))) return false;
        return true;
    }

    private PrayerSchedule() {}
}
