package com.devidea.timeleft

import android.app.Application
import com.devidea.timeleft.notification.ReminderScheduler
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        ReminderScheduler.createChannel(this)
    }
}