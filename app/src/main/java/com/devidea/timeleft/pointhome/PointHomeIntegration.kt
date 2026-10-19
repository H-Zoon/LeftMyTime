package com.devidea.timeleft.pointhome

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.avatye.pointhome.PointHomeSDK
import com.avatye.pointhome.builder.PointHomeActivityBuilder
import com.avatye.pointhome.builder.PointHomeServiceListener
import com.avatye.pointhome.core.utils.error.PointHomeError
import com.devidea.timeleft.BuildConfig
import com.devidea.timeleft.R

/** Owns SDK setup; the SDK owns the floating button's Activity lifecycle. */
object PointHomeIntegration {
    private const val TAG = "PointHome"
    private var initialized = false

    fun initialize(application: Application) {
        try {
            PointHomeSDK.initializer(
                application = application,
                listener = object : PointHomeSDK.InitializationListener {
                    override fun onSuccess() {
                        initialized = true
                    }

                    override fun onFailure(error: Throwable) {
                        initialized = false
                        Log.w(TAG, "Initialization failed: ${error.javaClass.simpleName}")
                    }
                },
            )
                .setLogTrace(use = false)
                .setDevelopMode(value = false)
        } catch (exception: Exception) {
            initialized = false
            Log.w(TAG, "Initialization failed: ${exception.javaClass.simpleName}")
        }
    }

    fun attachFloatingButton(activity: AppCompatActivity) {
        if (!initialized || activity.isFinishing || activity.isDestroyed) return
        if (BuildConfig.POINTHOME_SERVICE_ID.isBlank() || BuildConfig.POINTHOME_SECRET.isBlank()) {
            Log.i(TAG, "Credentials are not configured; floating button is disabled.")
            return
        }

        try {
            PointHomeActivityBuilder.with(
                activity = activity,
                serviceID = BuildConfig.POINTHOME_SERVICE_ID,
                secret = BuildConfig.POINTHOME_SECRET,
            )
                // Omit uniqueID, payload and baseUrl to use SDK defaults.
                .listener(object : PointHomeServiceListener {
                    override fun serviceOpen(caller: PointHomeSDK.CallResource) = Unit

                    override fun serviceClose(caller: PointHomeSDK.CallResource) = Unit

                    override fun openFail(reason: PointHomeError) {
                        // Do not log SDK error text, which may contain service/request details.
                        Log.w(TAG, "Service request failed: ${reason.errorCode}")
                    }
                })
                // Keep the default position, SEEOTTER character and enabled animation.
                .showAsFloatingButton(message = activity.getString(R.string.pointhome_floating_message)) { created ->
                    if (!created) Log.w(TAG, "Floating button creation failed.")
                }
        } catch (exception: Exception) {
            Log.w(TAG, "Floating button setup failed: ${exception.javaClass.simpleName}")
        }
    }
}
