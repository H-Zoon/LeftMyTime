package com.devidea.timeleft.widget

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class WidgetRefreshBatchTest {
    @Test fun `failed render still refreshes other widgets and requests a retry`() = runBlocking {
        val rendered = mutableListOf<Int>()
        val failed = mutableListOf<Int>()
        val complete = refreshWidgetBatch(intArrayOf(1, 2, 3), render = { id ->
            rendered += id
            if (id == 1) throw IllegalStateException("Render unavailable")
            true
        }, onFailure = { id, _ -> failed += id })
        assertFalse(complete)
        assertEquals(listOf(1, 2, 3), rendered)
        assertEquals(listOf(1), failed)
    }

    @Test fun `posted loading error does not count as a successful data refresh`() = runBlocking {
        val rendered = mutableListOf<Int>()
        assertFalse(refreshWidgetBatch(intArrayOf(1, 2), render = { id ->
            rendered += id
            id != 1
        }, onFailure = { _, _ -> fail("No render exception expected") }))
        assertEquals(listOf(1, 2), rendered)
    }

    @Test fun `worker cancellation is propagated instead of being swallowed as a render failure`() = runBlocking {
        val cancellation = CancellationException("Stopped by scheduler")
        val rendered = mutableListOf<Int>()
        try {
            refreshWidgetBatch(intArrayOf(1, 2), render = { id ->
                rendered += id
                throw cancellation
            }, onFailure = { _, _ -> fail("Cancellation must propagate") })
            fail("Expected cancellation")
        } catch (error: CancellationException) {
            assertSame(cancellation, error)
        }
        assertEquals(listOf(1), rendered)
    }

    @Test fun `successful and empty batches finish without a retry`() = runBlocking {
        assertTrue(refreshWidgetBatch(intArrayOf(1, 2), render = { true }, onFailure = { _, _ -> fail() }))
        assertTrue(refreshWidgetBatch(intArrayOf(), render = { fail("No widgets"); false }, onFailure = { _, _ -> fail() }))
    }
}
