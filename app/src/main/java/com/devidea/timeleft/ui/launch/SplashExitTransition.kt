package com.devidea.timeleft.ui.launch

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Build
import android.view.View
import android.view.animation.PathInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreenViewProvider
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.devidea.timeleft.R
import com.devidea.timeleft.ui.theme.Motion
import com.devidea.timeleft.ui.theme.Spacing

/** Runs only when the first app frame is ready; never holds launch for the logo animation. */
internal class SplashExitTransition(
    private val activity: AppCompatActivity,
    private val provider: SplashScreenViewProvider,
) : DefaultLifecycleObserver {
    private val animation = AnimatorSet()
    private var removed = false

    fun start() {
        if (!ValueAnimator.areAnimatorsEnabled() || activity.isFinishing || activity.isDestroyed ||
            !activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            finish()
            return
        }
        val overlay = provider.view
        // Platform getIconView() is nullable (for example after a permission-triggered
        // process restart). core-splashscreen 1.0.1 asserts non-null in its wrapper.
        val icon = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (overlay as? android.window.SplashScreenView)?.iconView
        } else provider.iconView
        if (icon == null) {
            finish()
            return
        }
        activity.lifecycle.addObserver(this)
        // A tap finishes the ready-to-enter transition; no extra gesture or waiting screen.
        overlay.contentDescription = activity.getString(R.string.splash_enter_app)
        overlay.setOnClickListener { finish() }
        val lift = Spacing.s.value * activity.resources.displayMetrics.density
        animation.apply {
            playTogether(
                ObjectAnimator.ofFloat(icon, View.SCALE_X, 1f, 0.96f),
                ObjectAnimator.ofFloat(icon, View.SCALE_Y, 1f, 0.96f),
                ObjectAnimator.ofFloat(icon, View.TRANSLATION_Y, 0f, -lift),
                ObjectAnimator.ofFloat(overlay, View.ALPHA, 1f, 0f),
            )
            duration = Motion.MediumMs.toLong()
            interpolator = PathInterpolator(0.2f, 0f, 0f, 1f)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) = finish()
                override fun onAnimationCancel(animation: Animator) = finish()
            })
            start()
        }
    }

    private fun finish() {
        if (removed) return
        removed = true
        animation.removeAllListeners()
        animation.cancel()
        provider.view.setOnClickListener(null)
        provider.remove()
        activity.lifecycle.removeObserver(this)
    }

    override fun onStop(owner: LifecycleOwner) = finish()
    override fun onDestroy(owner: LifecycleOwner) = finish()
}
