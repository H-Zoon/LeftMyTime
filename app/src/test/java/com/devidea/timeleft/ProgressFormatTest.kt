package com.devidea.timeleft

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressFormatTest {
    @Test
    fun `roundPercent is independent of decimal separator locale`() {
        val originalLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)

            assertEquals(32.7f, roundPercent(32.65f), EPSILON)
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun `formatPercent applies locale only to display text`() {
        assertEquals("32,7", formatPercent(32.65f, Locale.GERMANY))
        assertEquals("32.7", formatPercent(32.65f, Locale.US))
        assertEquals("100", formatPercent(100f, Locale.US))
    }

    companion object {
        private const val EPSILON = 0.001f
    }
}
