package com.devidea.timeleft.sharedclips

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.preference.PreferenceManager
import com.devidea.timeleft.BuildConfig
import com.devidea.timeleft.R
import com.devidea.timeleft.config.UserPreferences
import com.devidea.timeleft.ui.theme.TimeLeftTheme
import java.time.LocalDate

class SharedClipsActivity : AppCompatActivity() {
    private val model: SharedClipsModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!enabled) {
            finish()
            return
        }
        val event = intent.getStringExtra(EVENT_DATE)?.let { date ->
            AlbumEvent(intent.getIntExtra(EVENT_ID, 0), requireNotNull(intent.getStringExtra(EVENT_TITLE)), LocalDate.parse(date))
        }
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        setContent {
            val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { video ->
                if (video != null) model.select(video)
            }
            val onBack = { if (!model.goUp()) finish() }
            LaunchedEffect(event) { model.start(event) }
            BackHandler(onBack = onBack)
            TimeLeftTheme(
                themeMode = prefs.getString(UserPreferences.KEY_THEME, null) ?: UserPreferences.THEME_AUTO,
                paletteKey = prefs.getString(UserPreferences.KEY_COLOR_THEME, null) ?: UserPreferences.COLOR_THEME_CLAY,
                designKey = prefs.getString(UserPreferences.KEY_DESIGN, null) ?: UserPreferences.DESIGN_TIME_FOCUS,
            ) {
                SharedClipsScreen(
                    page = model.page,
                    busy = model.busy,
                    error = model.error,
                    invitation = model.invitation,
                    selection = model.selection,
                    onBack = onBack,
                    onRefresh = model::refresh,
                    onOpen = model::open,
                    onCreate = model::create,
                    onJoin = model::join,
                    onIssueInvitation = model::invite,
                    onShareInvitation = ::shareInvitation,
                    onPick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                    },
                    onUpload = model::upload,
                    onCancelSelection = model::clearSelection,
                    onDelete = model::delete,
                    onDismissError = model::clearError,
                )
            }
        }
    }

    private fun shareInvitation(code: String) {
        val message = getString(R.string.shared_clips_invite_message, code)
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, message)
        startActivity(Intent.createChooser(send, getString(R.string.shared_clips_invite)))
    }

    companion object {
        private const val EVENT_TITLE = "title"
        private const val EVENT_DATE = "date"
        private const val EVENT_ID = "local_id"

        internal val enabled: Boolean
            get() = BuildConfig.SHARED_CLIPS_URL.startsWith("https://") &&
                BuildConfig.SHARED_CLIPS_KEY.startsWith("sb_publishable_")

        internal fun launch(context: Context, event: AlbumEvent? = null) {
            val intent = Intent(context, SharedClipsActivity::class.java)
            if (event != null) {
                intent.putExtra(EVENT_ID, event.id)
                intent.putExtra(EVENT_TITLE, event.title)
                intent.putExtra(EVENT_DATE, event.date.toString())
            }
            context.startActivity(intent)
        }
    }
}
