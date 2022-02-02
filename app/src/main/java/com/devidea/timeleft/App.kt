package com.devidea.timeleft

import android.app.Application
import android.content.Context
import com.devidea.timeleft.datadase.AppDatabase

class App : Application() {

    init{
        instance = this
    }

    companion object {
        var instance: App? = null
        fun context() : Context {
            return instance!!.applicationContext
        }
    }

}