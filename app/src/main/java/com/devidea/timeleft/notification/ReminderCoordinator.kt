package com.devidea.timeleft.notification

import android.content.Context
import com.devidea.timeleft.repository.TimeLeftRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/** Owns refreshes from process/permission/system events independently of UI collection. */
@Singleton
class ReminderCoordinator @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val repository: TimeLeftRepository,
) {
    private val mutex = Mutex()
    suspend fun refresh(force: Boolean = false) = mutex.withLock {
        ReminderScheduler.rescheduleAll(context, repository.allItems(), force)
    }
}
