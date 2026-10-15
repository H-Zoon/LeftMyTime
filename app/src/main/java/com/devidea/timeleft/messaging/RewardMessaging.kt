package com.devidea.timeleft.messaging

import android.content.Context
import android.content.SharedPreferences
import android.os.Looper
import android.util.Log
import androidx.preference.PreferenceManager
import com.google.firebase.messaging.FirebaseMessaging
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Installation-local consent is authoritative. Notification permission is a separate device state. */
object RewardMessaging {
    private const val CONSENT_KEY = "reward_notifications_consent"
    private const val CONFIRMATION_TIMEOUT_SECONDS = 60L
    private val lock = Any()
    private val executor = Executors.newSingleThreadScheduledExecutor()
    private val consentLoaded = CountDownLatch(1)
    private val policy = RewardTopicSyncPolicy()
    private val mutableState = MutableStateFlow(RewardMessagingState())
    val state: StateFlow<RewardMessagingState> = mutableState.asStateFlow()

    private var applicationContext: Context? = null
    private var preferences: SharedPreferences? = null // Accessed only on the executor.
    private var persistedConsent: Boolean? = null // Accessed only on the executor.
    private var userChoseConsent = false

    fun initialize(context: Context) {
        val appContext = context.applicationContext
        synchronized(lock) {
            if (applicationContext != null) return
            applicationContext = appContext
        }
        // Preference initialization, channel I/O, and all SDK calls stay off the UI thread.
        executor.execute {
            try {
                val prefs = PreferenceManager.getDefaultSharedPreferences(appContext)
                preferences = prefs
                val saved = prefs.getBoolean(CONSENT_KEY, false)
                persistedConsent = saved
                synchronized(lock) {
                    // A choice made while the disk load was pending must win over the old value.
                    if (!userChoseConsent) policy.setConsent(saved)
                    publishLocked()
                }
                consentLoaded.countDown()
                refreshOnExecutor(appContext, retryFailed = true)
            } catch (_: Exception) {
                failLocally()
            } finally {
                consentLoaded.countDown()
            }
        }
    }

    /** Used on foreground return and by the explicit retry action; pending attempts are deduplicated. */
    fun refresh() {
        val context = synchronized(lock) { applicationContext } ?: return
        executor.execute { refreshOnExecutor(context, retryFailed = true) }
    }

    fun setConsent(granted: Boolean) {
        val context = synchronized(lock) {
            userChoseConsent = true
            policy.setConsent(granted)
            publishLocked() // Revocation blocks service-delivered rewards immediately.
            applicationContext
        } ?: return
        executor.execute {
            if (!granted) cancelRewardsIfStillOptedOut(context)
            refreshOnExecutor(context, retryFailed = true)
        }
    }

    /** register() may redeliver the same FID; only a changed identity invalidates a settled sync. */
    fun onRegistered(installationId: String) {
        val context = synchronized(lock) {
            policy.onRegistered(installationId)
            publishLocked()
            applicationContext
        } ?: return
        executor.execute { reconcileOnExecutor(context, retryFailed = false) }
    }

    /** Serializes the final display check with revocation, including cancellation of earlier posts. */
    internal fun postWithConsent(action: () -> PushNotificationResult): PushNotificationResult {
        // FirebaseMessagingService dispatches on a worker. A cold-start data message can arrive
        // before the separate preference read finishes; allow only that local read a short wait.
        // Future callers on the main thread must never wait, and timeout still defaults to denial.
        if (consentLoaded.count > 0L) {
            if (Looper.myLooper() == Looper.getMainLooper()) return PushNotificationResult.RewardOptedOut
            try {
                if (!consentLoaded.await(2, TimeUnit.SECONDS)) return PushNotificationResult.RewardOptedOut
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                return PushNotificationResult.RewardOptedOut
            }
        }
        return synchronized(lock) {
            if (!policy.consentGranted) PushNotificationResult.RewardOptedOut else action()
        }
    }

    private fun refreshOnExecutor(context: Context, retryFailed: Boolean) {
        try {
            PushNotifications.createRewardChannel(context)
            val allowed = PushNotifications.canPostRewardNotifications(context)
            synchronized(lock) {
                mutableState.value = mutableState.value.copy(notificationsAllowed = allowed)
            }
            cancelRewardsIfStillOptedOut(context)
            reconcileOnExecutor(context, retryFailed)
        } catch (_: Exception) {
            failLocally()
        }
    }

    private fun reconcileOnExecutor(context: Context, retryFailed: Boolean) {
        try {
            val consent = synchronized(lock) { policy.consentGranted }
            val prefs = preferences ?: PreferenceManager.getDefaultSharedPreferences(context).also {
                preferences = it
            }
            if (persistedConsent != consent) {
                // Synchronous disk confirmation is allowed here, never on the main thread.
                if (!prefs.edit().putBoolean(CONSENT_KEY, consent).commit()) {
                    failLocally()
                    return
                }
                persistedConsent = consent
            }
            val attempt = synchronized(lock) {
                // If consent changed during the disk save, the next queued reconciliation saves it
                // before issuing that generation's SDK operation.
                if (policy.consentGranted != persistedConsent) return
                policy.begin(retryFailed).also { publishLocked() }
            } ?: return
            // Do not await an earlier task: offline subscribe -> opt-out must append unsubscribe
            // immediately, so Firebase's persistent FIFO eventually ends at the newest choice.
            val messaging = FirebaseMessaging.getInstance()
            val task = if (attempt.consentGranted) {
                messaging.subscribeToTopic(PushNotifications.REWARD_CHANNEL_ID)
            } else {
                messaging.unsubscribeFromTopic(PushNotifications.REWARD_CHANNEL_ID)
            }
            val timeout = executor.schedule({
                synchronized(lock) {
                    policy.complete(attempt, success = false)
                    publishLocked()
                }
            }, CONFIRMATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            task.addOnCompleteListener(executor) { completed ->
                timeout.cancel(false)
                synchronized(lock) {
                    policy.complete(attempt, completed.isSuccessful)
                    publishLocked()
                }
            }
        } catch (_: Exception) {
            failLocally()
        }
    }

    private fun cancelRewardsIfStillOptedOut(context: Context) {
        synchronized(lock) {
            // An old opt-out runnable must not erase a reward posted after a later opt-in.
            // Posting and cancellation share this lock, including the final consent check.
            if (policy.consentGranted) return
            try { PushNotifications.cancelRewardNotifications(context) }
            catch (_: Exception) { Log.w("RewardMessaging", "Reward notification cleanup unavailable") }
        }
    }

    private fun failLocally() {
        synchronized(lock) {
            policy.localFailure()
            publishLocked()
        }
        // Avoid payloads, installation IDs, authentication values, and exception messages.
        Log.w("RewardMessaging", "Reward preference or topic synchronization unavailable")
    }

    private fun publishLocked() {
        mutableState.value = mutableState.value.copy(
            consentGranted = policy.consentGranted,
            topicSync = policy.status,
        )
    }
}
