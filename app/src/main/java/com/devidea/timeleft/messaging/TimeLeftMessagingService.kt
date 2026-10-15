package com.devidea.timeleft.messaging

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class TimeLeftMessagingService : FirebaseMessagingService() {
    override fun onRegistered(installationId: String) {
        super.onRegistered(installationId)
        PushMessaging.registered(installationId)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        try {
            PushMessaging.messageReceived(message)
            when (PushNotifications.show(applicationContext, message)) {
                PushNotificationResult.Posted -> PushMessaging.notificationResult(blocked = false)
                PushNotificationResult.Blocked -> PushMessaging.notificationResult(blocked = true)
                PushNotificationResult.RewardOptedOut -> Unit
            }
        } catch (exception: Exception) {
            PushMessaging.notificationFailed(exception)
        }
    }
}
