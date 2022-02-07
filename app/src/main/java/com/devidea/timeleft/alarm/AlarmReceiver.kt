package com.devidea.timeleft.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.app.NotificationCompat
import com.devidea.timeleft.App
import com.devidea.timeleft.R
import com.devidea.timeleft.activity.MainActivity
import com.devidea.timeleft.datadase.AppDatabase
import java.time.LocalDate

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val TAG = "AlarmReceiver"
        const val PRIMARY_CHANNEL_ID = "devidea_timeleft_alarm"
    }

    lateinit var notificationManager: NotificationManager

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received intent : $intent")
        Log.d("Received intent", intent.getIntExtra("id", 0).toString())
        notificationManager = context.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        createNotificationChannel()
        deliverNotification(context, intent)

    }

    private fun deliverNotification(context: Context, intent: Intent) {
        val NOTIFICATION_ID = intent.getIntExtra("id", 0)
        val title =
            AppDatabase.getDatabase(App.context()).itemDao().getSelectItem(NOTIFICATION_ID).summery
        val contentIntent = Intent(context, MainActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            contentIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        Log.d("Receiverflag", intent.getIntExtra("flag", 0).toString())
        if (2 == intent.getIntExtra("flag", 0)) {
            Log.d("weektest2", "ifin")
            if ((LocalDate.now()).dayOfWeek.value != 6 || (LocalDate.now()).dayOfWeek.value != 7 ){
                Log.d("weektest3", (LocalDate.now()).dayOfWeek.value.toString())
                val builder =
                    NotificationCompat.Builder(context, PRIMARY_CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle("일정 종료 알림")
                        .setContentText("$title 의 설정한 알림입니다. 확인하려면 터치하세요")
                        .setContentIntent(contentPendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .setDefaults(NotificationCompat.DEFAULT_ALL)

                notificationManager.notify(NOTIFICATION_ID, builder.build())
            }
        }else{
            val builder =
                NotificationCompat.Builder(context, PRIMARY_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle("일정 종료 알림")
                    .setContentText("$title 의 설정한 알림입니다. 확인하려면 터치하세요")
                    .setContentIntent(contentPendingIntent)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)

            notificationManager.notify(NOTIFICATION_ID, builder.build())
        }
    }

    fun createNotificationChannel() {
        val notificationChannel = NotificationChannel(
            PRIMARY_CHANNEL_ID,
            "Stand up notification",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationChannel.enableLights(true)
        //notificationChannel.lightColor = Color.RED
        notificationChannel.enableVibration(true)
        notificationChannel.description = "AlarmManager"
        notificationManager.createNotificationChannel(
            notificationChannel
        )
    }

}