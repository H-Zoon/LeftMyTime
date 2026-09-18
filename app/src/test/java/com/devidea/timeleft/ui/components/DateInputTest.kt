package com.devidea.timeleft.ui.components

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class DateInputTest {
    @Test fun `date input validates leap dates and field bounds`() {
        assertEquals(LocalDate.of(2024, 2, 29), parseDateInput("2024", "2", "29"))
        assertNull(parseDateInput("2026", "2", "29"))
        assertNull(parseDateInput("2026", "13", "1"))
        assertNull(parseDateInput("0", "1", "1"))
        assertNull(parseDateInput("2026", "", "1"))
    }
}
