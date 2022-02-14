package com.devidea.timeleft.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import androidx.core.app.NotificationCompat
import com.devidea.timeleft.App
import com.devidea.timeleft.R
import com.devidea.timeleft.activity.MainActivity
import com.devidea.timeleft.activity.MainActivity.Companion.prefs
import com.devidea.timeleft.datadase.AppDatabase
import java.time.LocalDate

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val TAG = "AlarmReceiver"
        const val PRIMARY_CHANNEL_ID = "devidea_timeleft_alarm"
    }

    lateinit var notificationManager: NotificationManager

    override fun onReceive(context: Context, intent: Intent) {

        val action = intent.action
        Log.d("onReceive", "onReceive() action = $action")

        notificationManager = context.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        if (!prefs.getBoolean("pause", false)) {
            createNotificationChannel()
            deliverNotification(context, intent)
        }
    }


    private fun deliverNotification(context: Context, intent: Intent) {

        val action = intent.action
        Log.d("deliverNotification", "onReceive() action = $action")

        val NOTIFICATION_ID = intent.getIntExtra("id", 0)
        val title = intent.getStringExtra("title")
        val contentIntent = Intent(context, MainActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder =
            NotificationCompat.Builder(context, PRIMARY_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("일정 종료 알림")
                .setContentText("$title 의 설정한 알림입니다. 확인하려면 터치하세요")
                .setContentIntent(contentPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)

        if (2 == intent.getIntExtra("flag", 0)) {
            if ((LocalDate.now()).dayOfWeek.value != 6 || (LocalDate.now()).dayOfWeek.value != 7) {
                notificationManager.notify(NOTIFICATION_ID, builder.build())
            }
        } else {
            notificationManager.notify(NOTIFICATION_ID, builder.build())
        }
    }
    /*

    private fun Foo(intent: Intent) {
        val day: Long = 86400000
        val setDay: Long = intent.getStringExtra("timeInMillis")!!.toLong()
        val pendingIntent = PendingIntent.getBroadcast(
            App.context(),
            intent.getIntExtra("id", 0),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = App.context().getSystemService(Context.ALARM_SERVICE) as AlarmManager

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            setDay + day,
            pendingIntent
        )
    }

     */

    private fun createNotificationChannel() {
        val notificationChannel = NotificationChannel(
            PRIMARY_CHANNEL_ID,
            "TimeLeft notification",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationChannel.enableLights(true)
        notificationChannel.lightColor = Color.RED
        notificationChannel.enableVibration(true)
        notificationChannel.description = "AlarmManager"
        notificationManager.createNotificationChannel(
            notificationChannel
        )
    }

}