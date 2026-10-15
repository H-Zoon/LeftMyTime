package com.devidea.timeleft.messaging

import android.content.Context
import android.util.Log
import com.devidea.timeleft.BuildConfig
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PushMessagingState(
    val registering: Boolean = false,
    val installationId: String? = null,
    val registeredAtMillis: Long? = null,
    val lastReceivedAtMillis: Long? = null,
    val lastMessageId: String? = null,
    val lastDataKeyCount: Int = 0,
    val error: String? = null,
    val notificationBlocked: Boolean = false,
)

/** Diagnostics are kept only for the current process; Firebase owns registration persistence. */
object PushMessaging {
    internal const val LOG_TAG = "TimeLeftFCM"
    private const val REGISTRATION_ERROR = "registration"
    private const val NOTIFICATION_ERROR = "notification"
    private val registrationLock = Any()
    private val mutableState = MutableStateFlow(PushMessagingState())

    val state: StateFlow<PushMessagingState> = mutableState.asStateFlow()

    @Volatile private var applicationContext: Context? = null

    fun initialize(context: Context) {
        try {
            applicationContext = context.applicationContext
            register()
        } catch (exception: Exception) {
            registrationFailed(exception)
        }
    }

    /** register() re-delivers onRegistered with the current FID, even if already registered. */
    fun register() {
        val context = applicationContext
        if (context == null) {
            registrationFailed(IllegalStateException())
            return
        }
        synchronized(registrationLock) {
            if (mutableState.value.registering) return
            mutableState.update { it.copy(registering = true, error = null) }
        }

        // Notification permission does not prevent FCM registration or message reception.
        try {
            PushNotifications.createChannel(context)
            notificationResult(blocked = !PushNotifications.canPostNotifications(context))
        } catch (exception: Exception) {
            notificationFailed(exception)
        }

        try {
            FirebaseMessaging.getInstance().register()
                .addOnFailureListener(::registrationFailed)
        } catch (exception: Exception) {
            registrationFailed(exception)
        }
    }

    internal fun registered(installationId: String) {
        mutableState.update {
            it.copy(
                registering = false,
                installationId = installationId,
                registeredAtMillis = System.currentTimeMillis(),
                error = it.error?.takeUnless { error -> error.startsWith("$REGISTRATION_ERROR:") },
            )
        }
        RewardMessaging.onRegistered(installationId)
        if (BuildConfig.DEBUG) Log.d(LOG_TAG, "FCM registration completed")
    }

    internal fun messageReceived(message: RemoteMessage) {
        mutableState.update {
            it.copy(
                lastReceivedAtMillis = System.currentTimeMillis(),
                lastMessageId = message.messageId,
                lastDataKeyCount = message.data.size,
            )
        }
        // Do not log sender, title, body, data keys, or payload values.
        Log.d(LOG_TAG, "Message received; data key count: ${message.data.size}")
    }

    internal fun notificationResult(blocked: Boolean) {
        mutableState.update {
            it.copy(
                notificationBlocked = blocked,
                error = it.error?.takeUnless { error -> error.startsWith("$NOTIFICATION_ERROR:") },
            )
        }
        if (blocked) Log.i(LOG_TAG, "Notification display is blocked by device settings")
    }

    internal fun notificationFailed(exception: Exception) {
        mutableState.update {
            it.copy(
                notificationBlocked = if (exception is SecurityException) true else it.notificationBlocked,
                error = safeError(NOTIFICATION_ERROR, exception),
            )
        }
        Log.w(LOG_TAG, "Notification operation failed: ${exception.javaClass.simpleName}")
    }

    private fun registrationFailed(exception: Exception) {
        mutableState.update {
            it.copy(registering = false, error = safeError(REGISTRATION_ERROR, exception))
        }
        Log.w(LOG_TAG, "Registration failed: ${exception.javaClass.simpleName}")
    }

    private fun safeError(operation: String, exception: Exception): String =
        "$operation: ${exception.javaClass.simpleName}"
}
