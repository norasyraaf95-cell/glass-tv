package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;
import java.time.LocalDate;
import java.time.LocalTime;

public class PrayerScheduleTest {
    @Test public void parsesJakimDateAndTime() {
        assertEquals(LocalDate.of(2026, 11, 28), PrayerSchedule.parseDate("28-Nov-2026"));
        assertEquals("05:52", PrayerSchedule.display(PrayerSchedule.parseTime("05:52:00")));
        assertEquals("19:03", PrayerSchedule.display(PrayerSchedule.parseTime("19:03")));
    }

    @Test public void highlightsNextPrayerAndNeverTodayAfterIsyak() {
        LocalTime[] times = {LocalTime.of(5,52), LocalTime.of(13,5), LocalTime.of(16,29),
                LocalTime.of(19,3), LocalTime.of(20,17)};
        assertTrue(PrayerSchedule.ordered(times));
        assertEquals(0, PrayerSchedule.next(times, LocalTime.of(4,0)));
        assertEquals(3, PrayerSchedule.next(times, LocalTime.of(18,45)));
        assertEquals(3, PrayerSchedule.next(times, LocalTime.of(19,3)));
        assertEquals(4, PrayerSchedule.next(times, LocalTime.of(19,4)));
        assertEquals(-1, PrayerSchedule.next(times, LocalTime.of(21,0)));
        times[1] = times[0];
        assertFalse(PrayerSchedule.ordered(times));
    }
}
