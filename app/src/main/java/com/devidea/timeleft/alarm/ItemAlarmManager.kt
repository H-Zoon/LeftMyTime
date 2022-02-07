package com.devidea.timeleft.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context.ALARM_SERVICE
import com.devidea.timeleft.datadase.AppDatabase
import com.devidea.timeleft.datadase.itemdata.ItemEntity
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.util.Log
import com.devidea.timeleft.App
import com.devidea.timeleft.alarm.AlarmReceiver.Companion.TAG
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

class ItemAlarmManager {
    val intent = Intent(App.context(), AlarmReceiver::class.java)
    private lateinit var pendingIntent: PendingIntent

    fun alarmInit() {
        val alarmManager = App.context().getSystemService(ALARM_SERVICE) as AlarmManager
        val itemList: List<ItemEntity> = AppDatabase.getDatabase(App.context()).itemDao().item
        for (i in itemList.indices) {
            if ((itemList[i].alarmFlag != 0)) {
                //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                intent.putExtra("id", itemList[i].id)
                intent.putExtra("flag", itemList[i].alarmFlag)
                Log.d("flag", itemList[i].alarmFlag.toString())
                    this.pendingIntent = PendingIntent.getBroadcast(
                        App.context(),
                        itemList[i].id,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                when (itemList[i].type) {
                    "Month" -> {
                        val triggerTime = LocalDate.parse(
                            itemList[i].endValue,
                            DateTimeFormatter.ofPattern("yyyy-M-d")
                        ).atStartOfDay().minusDays(itemList[i].alarmRate.toLong())
                        Log.d("milli  M",  triggerTime.atZone(ZoneId.systemDefault())
                            .toInstant().toEpochMilli().toString())
                        alarmManager.set(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime.atZone(ZoneId.systemDefault())
                                .toInstant().toEpochMilli(),
                            pendingIntent
                        )
                    }

                    "Time" -> {
                        val triggerTime = LocalTime.parse(
                            itemList[i].endValue,
                            DateTimeFormatter.ofPattern("H:m")
                        ).minusHours(itemList[i].alarmRate.toLong())
                        val calendar: Calendar = Calendar.getInstance().apply {
                            timeInMillis = System.currentTimeMillis()
                            set(Calendar.HOUR_OF_DAY, triggerTime.hour)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                        }

                        Log.d("Timeto",  triggerTime.hour.toString())
                        Log.d("milli  T",  calendar.timeInMillis.toString())
                        Log.d("milliyto",  System.currentTimeMillis().toString())

                        alarmManager.setRepeating( AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            AlarmManager.INTERVAL_DAY,
                            pendingIntent)

                         /*
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )

                         */
                    }
                }
            }
            /* else {
                val alarmManager = App.context().getSystemService(ALARM_SERVICE) as AlarmManager

                val pendingIntent =
                    PendingIntent.getBroadcast(
                        App.context(),
                        itemList[i].id,
                        intent.putExtra("id", itemList[i].id),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                if (pendingIntent != null && alarmManager != null) {
                    alarmManager.cancel(pendingIntent)
                }
            }

             */
        }
    }

    fun alarmDelete(id: Int) {
        val alarmManager = App.context().getSystemService(ALARM_SERVICE) as AlarmManager
        Log.d(TAG, "delete call")
        val pendingIntent = PendingIntent.getBroadcast(
            App.context(),
            id,
            intent.putExtra("id", id),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            Log.d(TAG, "delete")
        }

    }


}