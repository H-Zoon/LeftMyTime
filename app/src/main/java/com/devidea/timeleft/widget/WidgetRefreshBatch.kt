package com.devidea.timeleft.widget

import kotlinx.coroutines.CancellationException

/** Attempt every widget, but tell the persistent worker when any of them needs a retry. */
internal suspend fun refreshWidgetBatch(
    ids: IntArray,
    render: suspend (Int) -> Boolean,
    onFailure: (Int, Exception) -> Unit,
): Boolean {
    var complete = true
    for (id in ids) {
        try {
            if (!render(id)) complete = false
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            complete = false
            onFailure(id, error)
        }
    }
    return complete
}
