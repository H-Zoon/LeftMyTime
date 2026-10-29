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
                .setLogTrace(use = true)
                .setDevelopMode(value = false)
        } catch (exception: Exception) {
            initialized = false
            Log.w(TAG, "Initialization failed: ${exception.javaClass.simpleName}")
        }
    }

    /** Retain the returned builder to keep its service listener alive. Creation is not an open. */
    fun attachFloatingButton(
        activity: AppCompatActivity,
        onOpened: () -> Unit,
        onResult: (created: Boolean) -> Unit = {},
    ): PointHomeActivityBuilder? {
        if (!canUse(activity)) return null
        return try {
            // Keep the default position, SEEOTTER character and enabled animation.
            createBuilder(activity, PointHomeSDK.CallResource.BUTTON, onOpened).also { builder ->
                builder.showAsFloatingButton(/*message = activity.getString(R.string.pointhome_floating_message)*/) { created ->
                    if (!created) Log.w(TAG, "Floating button creation failed.")
                    onResult(created)
                }
            }
        } catch (exception: Exception) {
            Log.w(TAG, "Floating button setup failed: ${exception.javaClass.simpleName}")
            null
        }
    }

    /** start() enqueues a launch; the owner retains the builder to keep its SDK listener alive. */
    fun open(activity: AppCompatActivity, onOpened: () -> Unit, onFailure: () -> Unit): PointHomeActivityBuilder? {
        var failureReported = false
        fun reportFailure() {
            if (failureReported) return
            failureReported = true
            onFailure()
        }
        if (!canUse(activity)) {
            reportFailure()
            return null
        }
        return try {
            createBuilder(activity, PointHomeSDK.CallResource.NATIVE, onOpened, ::reportFailure).also { it.start() }
        } catch (exception: Exception) {
            Log.w(TAG, "Service launch failed: ${exception.javaClass.simpleName}")
            reportFailure()
            null
        }
    }

    private fun canUse(activity: AppCompatActivity): Boolean {
        if (!initialized || activity.isFinishing || activity.isDestroyed) return false
        if (BuildConfig.POINTHOME_SERVICE_ID.isBlank() || BuildConfig.POINTHOME_SECRET.isBlank()) {
            Log.i(TAG, "Credentials are not configured; PointHome is unavailable.")
            return false
        }
        return true
    }

    private fun createBuilder(
        activity: AppCompatActivity,
        expectedCaller: PointHomeSDK.CallResource,
        onOpened: () -> Unit,
        onFailure: () -> Unit = {},
    ): PointHomeActivityBuilder = PointHomeActivityBuilder.with(
        activity = activity,
        serviceID = BuildConfig.POINTHOME_SERVICE_ID,
        secret = BuildConfig.POINTHOME_SECRET,
    )
        // Omit uniqueID, payload and baseUrl to use SDK defaults.
        .listener(object : PointHomeServiceListener {
            private var openReported = false

            override fun serviceOpen(caller: PointHomeSDK.CallResource) {
                if (caller != expectedCaller || openReported) return
                openReported = true
                onOpened()
            }

            override fun serviceClose(caller: PointHomeSDK.CallResource) {
                if (caller == expectedCaller) openReported = false
            }

            override fun openFail(reason: PointHomeError) {
                // Do not log SDK error text, which may contain service/request details.
                Log.w(TAG, "Service request failed: ${reason.errorCode}")
                onFailure()
            }
        })
}
