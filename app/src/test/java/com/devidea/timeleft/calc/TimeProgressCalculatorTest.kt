package com.devidea.timeleft.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import com.devidea.timeleft.database.itemdata.RecurrenceMode
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

class TimeProgressCalculatorTest {

    // ---------- dayProgress ----------

    @Test
    fun `dayProgress at midnight is zero percent and 23h59m59s left`() {
        val r = TimeProgressCalculator.dayProgress(LocalTime.MIDNIGHT)
        assertEquals(0f, r.percentElapsed, EPSILON)
        assertEquals(86_399L, r.durationLeft.seconds)
    }

    @Test
    fun `dayProgress at noon is 50 percent`() {
        val r = TimeProgressCalculator.dayProgress(LocalTime.NOON)
        assertEquals(50f, r.percentElapsed, EPSILON)
        assertEquals(Duration.ofHours(11).plusMinutes(59).plusSeconds(59).seconds, r.durationLeft.seconds)
    }

    @Test
    fun `dayProgress at end of day approaches 100 percent with zero seconds left`() {
        val r = TimeProgressCalculator.dayProgress(LocalTime.of(23, 59, 59))
        assertTrue("percent should be just under 100, was ${r.percentElapsed}", r.percentElapsed > 99.99f)
        assertEquals(0L, r.durationLeft.seconds)
    }

    // ---------- yearProgress ----------

    @Test
    fun `yearProgress on Jan 1 of leap year`() {
        val r = TimeProgressCalculator.yearProgress(LocalDate.of(2024, 1, 1))
        assertEquals(1f / 366f * 100f, r.percentElapsed, EPSILON)
        assertEquals(365, r.daysLeft)
    }

    @Test
    fun `yearProgress on Dec 31 of common year is 100 percent`() {
        val r = TimeProgressCalculator.yearProgress(LocalDate.of(2025, 12, 31))
        assertEquals(100f, r.percentElapsed, EPSILON)
        assertEquals(0, r.daysLeft)
    }

    @Test
    fun `yearProgress accounts for leap year length`() {
        val r = TimeProgressCalculator.yearProgress(LocalDate.of(2024, 2, 29))
        assertEquals(60f / 366f * 100f, r.percentElapsed, EPSILON)
        assertEquals(306, r.daysLeft)
    }

    // ---------- monthProgress ----------

    @Test
    fun `monthProgress on day 1 of 31-day month`() {
        val r = TimeProgressCalculator.monthProgress(LocalDate.of(2025, 1, 1))
        assertEquals(1f / 31f * 100f, r.percentElapsed, EPSILON)
        assertEquals(30, r.daysLeft)
    }

    @Test
    fun `monthProgress on Feb 29 of leap year is 100 percent`() {
        val r = TimeProgressCalculator.monthProgress(LocalDate.of(2024, 2, 29))
        assertEquals(100f, r.percentElapsed, EPSILON)
        assertEquals(0, r.daysLeft)
    }

    @Test
    fun `monthProgress on Feb 28 of common year is 100 percent`() {
        val r = TimeProgressCalculator.monthProgress(LocalDate.of(2025, 2, 28))
        assertEquals(100f, r.percentElapsed, EPSILON)
        assertEquals(0, r.daysLeft)
    }

    // ---------- customTimeProgress ----------

    @Test
    fun `customTimeProgress is Idle before start`() {
        val r = TimeProgressCalculator.customTimeProgress(
            start = LocalTime.of(9, 0),
            end = LocalTime.of(17, 0),
            now = LocalTime.of(8, 0)
        )
        assertEquals(CustomTimeProgress.Idle, r)
    }

    @Test
    fun `customTimeProgress is Idle at exact start (strict isAfter)`() {
        val r = TimeProgressCalculator.customTimeProgress(
            start = LocalTime.of(9, 0),
            end = LocalTime.of(17, 0),
            now = LocalTime.of(9, 0)
        )
        assertEquals(CustomTimeProgress.Idle, r)
    }

    @Test
    fun `customTimeProgress is Idle at exact end (strict isBefore)`() {
        val r = TimeProgressCalculator.customTimeProgress(
            start = LocalTime.of(9, 0),
            end = LocalTime.of(17, 0),
            now = LocalTime.of(17, 0)
        )
        assertEquals(CustomTimeProgress.Idle, r)
    }

    @Test
    fun `customTimeProgress at midpoint is 50 percent`() {
        val r = TimeProgressCalculator.customTimeProgress(
            start = LocalTime.of(9, 0),
            end = LocalTime.of(17, 0),
            now = LocalTime.of(13, 0)
        )
        assertTrue(r is CustomTimeProgress.Active)
        r as CustomTimeProgress.Active
        assertEquals(50f, r.percentElapsed, EPSILON)
        assertEquals(Duration.ofHours(4).seconds, r.durationLeft.seconds)
    }

    @Test
    fun `customTimeProgress is Idle after end`() {
        val r = TimeProgressCalculator.customTimeProgress(
            start = LocalTime.of(9, 0),
            end = LocalTime.of(17, 0),
            now = LocalTime.of(18, 0)
        )
        assertEquals(CustomTimeProgress.Idle, r)
    }

    // ---------- customDateProgress ----------

    @Test
    fun `customDateProgress at midpoint`() {
        val r = TimeProgressCalculator.customDateProgress(
            start = LocalDate.of(2025, 6, 1),
            end = LocalDate.of(2025, 6, 11),
            today = LocalDate.of(2025, 6, 6)
        )
        assertEquals(50f, r.percentElapsed, EPSILON)
        assertEquals(5, r.daysLeft)
        assertEquals(10, r.daysBetween)
    }

    @Test
    fun `customDateProgress at exact end is 100 percent`() {
        val r = TimeProgressCalculator.customDateProgress(
            start = LocalDate.of(2025, 6, 1),
            end = LocalDate.of(2025, 6, 11),
            today = LocalDate.of(2025, 6, 11)
        )
        assertEquals(100f, r.percentElapsed, EPSILON)
        assertEquals(0, r.daysLeft)
    }

    @Test
    fun `customDateProgress before start gives negative percent and positive daysLeft`() {
        val r = TimeProgressCalculator.customDateProgress(
            start = LocalDate.of(2025, 6, 1),
            end = LocalDate.of(2025, 6, 30),
            today = LocalDate.of(2025, 5, 25)
        )
        assertTrue(r.percentElapsed < 0f)
        assertEquals(36, r.daysLeft)
        assertEquals(29, r.daysBetween)
    }

    @Test
    fun `customDateProgress past end gives over 100 percent and negative daysLeft`() {
        val r = TimeProgressCalculator.customDateProgress(
            start = LocalDate.of(2025, 6, 1),
            end = LocalDate.of(2025, 6, 11),
            today = LocalDate.of(2025, 6, 20)
        )
        assertTrue(r.percentElapsed > 100f)
        assertEquals(-9, r.daysLeft)
    }

    @Test
    fun `customDateProgress with zero-length range is complete`() {
        val r = TimeProgressCalculator.customDateProgress(
            start = LocalDate.of(2025, 6, 1),
            end = LocalDate.of(2025, 6, 1),
            today = LocalDate.of(2025, 6, 1)
        )
        assertEquals(100f, r.percentElapsed, EPSILON)
        assertEquals(0, r.daysLeft)
        assertEquals(0, r.daysBetween)
    }

    // ---------- nextRecurrence ----------

    @Test
    fun `nextRecurrence returns null when today is on currentEnd`() {
        val r = TimeProgressCalculator.nextRecurrence(
            currentStart = LocalDate.of(2025, 6, 23),
            currentEnd = LocalDate.of(2025, 6, 30),
            today = LocalDate.of(2025, 6, 30),
            updateFlag = RecurrenceMode.Day,
            updateRate = 7
        )
        assertNull(r)
    }

    @Test
    fun `nextRecurrence returns null when updateFlag is None`() {
        val r = TimeProgressCalculator.nextRecurrence(
            currentStart = LocalDate.of(2025, 6, 23),
            currentEnd = LocalDate.of(2025, 6, 30),
            today = LocalDate.of(2025, 7, 5),
            updateFlag = RecurrenceMode.None,
            updateRate = 7
        )
        assertNull(r)
    }

    @Test
    fun `nextRecurrence Day preserves cycle length and inserts gap after end`() {
        // currentStart=5/1, currentEnd=5/10 → length 9 days.
        // updateRate=3 → 3-day gap after end → newStart=5/13, newEnd=5/22 (still 9 days).
        val r = TimeProgressCalculator.nextRecurrence(
            currentStart = LocalDate.of(2025, 5, 1),
            currentEnd = LocalDate.of(2025, 5, 10),
            today = LocalDate.of(2025, 5, 11),
            updateFlag = RecurrenceMode.Day,
            updateRate = 3
        )
        assertEquals(LocalDate.of(2025, 5, 13), r?.newStart)
        assertEquals(LocalDate.of(2025, 5, 22), r?.newEnd)
    }

    @Test
    fun `nextRecurrence Month lands on next day-of-month after currentEnd`() {
        // currentStart=5/1, currentEnd=5/10 → length 9 days. updateRate=20.
        // First day-20 strictly after 5/10 is 5/20 → newEnd=5/29.
        val r = TimeProgressCalculator.nextRecurrence(
            currentStart = LocalDate.of(2025, 5, 1),
            currentEnd = LocalDate.of(2025, 5, 10),
            today = LocalDate.of(2025, 5, 11),
            updateFlag = RecurrenceMode.Month,
            updateRate = 20
        )
        assertEquals(LocalDate.of(2025, 5, 20), r?.newStart)
        assertEquals(LocalDate.of(2025, 5, 29), r?.newEnd)
    }

    @Test
    fun `nextRecurrence Month rolls to next month when current month's day-of-month has passed`() {
        // currentEnd=5/25, updateRate=20 → day 20 in May has passed → next is 6/20.
        val r = TimeProgressCalculator.nextRecurrence(
            currentStart = LocalDate.of(2025, 5, 1),
            currentEnd = LocalDate.of(2025, 5, 25),
            today = LocalDate.of(2025, 5, 26),
            updateFlag = RecurrenceMode.Month,
            updateRate = 20
        )
        assertEquals(LocalDate.of(2025, 6, 20), r?.newStart)
    }

    @Test
    fun `nextRecurrence Month skips Feb when updateRate exceeds Feb length`() {
        // currentEnd=2/15 2025, updateRate=31 → Feb has 28 days → next is 3/31.
        val r = TimeProgressCalculator.nextRecurrence(
            currentStart = LocalDate.of(2025, 2, 1),
            currentEnd = LocalDate.of(2025, 2, 15),
            today = LocalDate.of(2025, 2, 16),
            updateFlag = RecurrenceMode.Month,
            updateRate = 31
        )
        assertEquals(LocalDate.of(2025, 3, 31), r?.newStart)
    }

    @Test
    fun `nextRecurrence Month crosses year boundary on Dec 31 with updateRate 31`() {
        // currentEnd=12/31 2024, updateRate=31 → next day-31 is Jan 31 2025.
        val r = TimeProgressCalculator.nextRecurrence(
            currentStart = LocalDate.of(2024, 12, 1),
            currentEnd = LocalDate.of(2024, 12, 31),
            today = LocalDate.of(2025, 1, 1),
            updateFlag = RecurrenceMode.Month,
            updateRate = 31
        )
        assertEquals(LocalDate.of(2025, 1, 31), r?.newStart)
    }

    @Test
    fun `nextRecurrence Month when currentEnd equals updateRate moves to next valid month`() {
        // currentEnd=5/20, updateRate=20 → day 20 of May has not passed (it IS the end),
        // but new start must be strictly after currentEnd → first eligible is 6/20.
        val r = TimeProgressCalculator.nextRecurrence(
            currentStart = LocalDate.of(2025, 5, 1),
            currentEnd = LocalDate.of(2025, 5, 20),
            today = LocalDate.of(2025, 5, 21),
            updateFlag = RecurrenceMode.Month,
            updateRate = 20
        )
        assertEquals(LocalDate.of(2025, 6, 20), r?.newStart)
    }

    companion object {
        private const val EPSILON = 0.001f
    }
}
