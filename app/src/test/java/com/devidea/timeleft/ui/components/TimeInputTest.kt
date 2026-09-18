package com.devidea.timeleft.ui.components

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalTime

class TimeInputTest {
    @Test fun `12 hour conversion distinguishes noon and midnight without accepting invalid input`() {
        assertEquals(LocalTime.MIDNIGHT, parseTimeInput("12", "00", false, false))
        assertEquals(LocalTime.NOON, parseTimeInput("12", "00", false, true))
        assertEquals(LocalTime.of(14, 35), parseTimeInput("2", "35", false, true))
        assertEquals(LocalTime.of(23, 59), parseTimeInput("23", "59", true, false))
        assertNull(parseTimeInput("0", "30", false, false))
        assertNull(parseTimeInput("24", "00", true, false))
        assertNull(parseTimeInput("12", "60", true, false))
        assertNull(parseTimeInput("", "00", true, false))
    }
}
