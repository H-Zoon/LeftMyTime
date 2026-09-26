package com.devidea.timeleft.input

import com.devidea.timeleft.database.itemdata.ItemType
import com.devidea.timeleft.database.itemdata.RecurrenceMode
import org.junit.Assert.*
import org.junit.Test

class SchedulePhraseParserTest {
    @Test fun focusIsOneOffAndBoundsAreExplicit() {
        assertEquals(SchedulePhrase.Focus(25), SchedulePhraseParser.parse("25분 집중"))
        assertEquals(SchedulePhrase.Focus(50), SchedulePhraseParser.parse("focus 50 minutes"))
        assertNull(SchedulePhraseParser.parse("focus 0m"))
        assertNull(SchedulePhraseParser.parse("focus 1441m"))
    }

    @Test fun overnightAndRecurrenceMustBeExplicit() {
        val item = (SchedulePhraseParser.parse("평일 22:00-다음 날 01:00 공부") as SchedulePhrase.Draft).item
        assertEquals(31, item.weekdays)
        assertTrue(item.endNextDay)
        assertEquals(RecurrenceMode.TimeRange, item.updateFlag)
        assertNull(SchedulePhraseParser.parse("daily 22:00-01:00 Study"))
        assertNull(SchedulePhraseParser.parse("09:00-10:00 Study"))
        assertNull(SchedulePhraseParser.parse("daily 24:00-25:00 Study"))
    }

    @Test fun exactDateRangeDoesNotInventRecurrence() {
        val item = (SchedulePhraseParser.parse("2028-02-29 to 2028-03-02 Trip") as SchedulePhrase.Draft).item
        assertEquals(ItemType.Date, item.type)
        assertEquals(RecurrenceMode.None, item.updateFlag)
        assertEquals("2028-02-29", item.startValue)
        assertNull(SchedulePhraseParser.parse("2027-02-29 to 2027-03-02 Trip"))
        assertNull(SchedulePhraseParser.parse("2026-10-10 to 2026-10-01 Trip"))
        assertNull(SchedulePhraseParser.parse("tomorrow morning study"))
        assertNull(SchedulePhraseParser.parse("매일 09:00-10:00"))
    }
}
