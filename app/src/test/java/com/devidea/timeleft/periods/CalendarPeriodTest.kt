package com.devidea.timeleft.periods

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class CalendarPeriodTest {
    @Test fun mondayWeekCanSpanTwoYearsAndSundayStaysInThatWeek() {
        val expected = LocalDate.parse("2025-12-29") to LocalDate.parse("2026-01-04")
        assertEquals(expected, CalendarPeriod.Week.bounds(LocalDate.parse("2026-01-01")))
        assertEquals(expected, CalendarPeriod.Week.bounds(LocalDate.parse("2026-01-04")))
        assertEquals(LocalDate.parse("2026-01-05"), CalendarPeriod.Week.bounds(LocalDate.parse("2026-01-05")).first)
    }

    @Test fun quarterUsesActualCalendarLengthIncludingLeapDay() {
        val (start, end) = CalendarPeriod.Quarter.bounds(LocalDate.parse("2028-02-29"))
        assertEquals(LocalDate.parse("2028-01-01"), start)
        assertEquals(LocalDate.parse("2028-03-31"), end)
        assertEquals(91L, ChronoUnit.DAYS.between(start, end) + 1)
        assertEquals(LocalDate.parse("2028-04-01"), CalendarPeriod.Quarter.bounds(LocalDate.parse("2028-04-01")).first)
        assertEquals(LocalDate.parse("2028-12-31"), CalendarPeriod.Quarter.bounds(LocalDate.parse("2028-12-31")).second)
    }
}
