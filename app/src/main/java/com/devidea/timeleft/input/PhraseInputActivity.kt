package com.devidea.timeleft.input

import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devidea.timeleft.R
import com.devidea.timeleft.activity.ItemEditorActivity
import com.devidea.timeleft.focus.FocusActivity
import com.devidea.timeleft.focus.FocusViewModel
import com.devidea.timeleft.preferences.UserPreferences
import com.devidea.timeleft.ui.components.TimeLeftTopAppBar
import com.devidea.timeleft.ui.components.TimeLeftUnderlineTextField
import com.devidea.timeleft.ui.theme.*
import com.devidea.timeleft.widget.widgetScheduleDescription
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PhraseInputActivity : AppCompatActivity() {
    @Inject lateinit var prefs: SharedPreferences
    private val focus: FocusViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var text by rememberSaveable { mutableStateOf("") }
            var reviewed by rememberSaveable { mutableStateOf<String?>(null) }
            val result = remember(reviewed) { reviewed?.let(SchedulePhraseParser::parse) }
            val state by focus.state.collectAsStateWithLifecycle()
            TimeLeftTheme(designKey = prefs.getString(UserPreferences.KEY_DESIGN, UserPreferences.DESIGN_TIME_FOCUS) ?: UserPreferences.DESIGN_TIME_FOCUS,
                themeMode = prefs.getString(UserPreferences.KEY_THEME, UserPreferences.THEME_AUTO) ?: UserPreferences.THEME_AUTO,
                paletteKey = prefs.getString(UserPreferences.KEY_COLOR_THEME, UserPreferences.COLOR_THEME_CLAY) ?: UserPreferences.COLOR_THEME_CLAY) {
                Scaffold(containerColor = MaterialTheme.colorScheme.background,
                    topBar = { TimeLeftTopAppBar(stringResource(R.string.phrase_title), ::finish) }) { padding ->
                    Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                        .padding(horizontal = LayoutTokens.ScreenHorizontal, vertical = Spacing.l),
                        verticalArrangement = Arrangement.spacedBy(Spacing.l)) {
                        Text(stringResource(R.string.phrase_explanation), style = MaterialTheme.typography.bodyMedium)
                        TimeLeftUnderlineTextField(text, { if (it.length <= 512) { text = it; reviewed = null } },
                            label = { Text(stringResource(R.string.phrase_field)) }, modifier = Modifier.fillMaxWidth())
                        Button(onClick = { reviewed = text }, enabled = text.isNotBlank() && !state.busy,
                            modifier = Modifier.heightIn(min = LayoutTokens.MinTouchTarget)) { Text(stringResource(R.string.phrase_interpret)) }
                        if (reviewed != null) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            when (result) {
                                null -> Text(stringResource(R.string.phrase_unsupported), color = MaterialTheme.colorScheme.error)
                                is SchedulePhrase.Draft -> {
                                    Text(result.item.title, style = MaterialTheme.typography.titleLarge)
                                    Text(widgetScheduleDescription(this@PhraseInputActivity, result.item))
                                    Text(stringResource(R.string.phrase_review_hint), style = MaterialTheme.typography.bodyMedium)
                                    Button(onClick = { startActivity(ItemEditorActivity.draftIntent(this@PhraseInputActivity, reviewed.orEmpty())) },
                                        modifier = Modifier.heightIn(min = LayoutTokens.MinTouchTarget)) { Text(stringResource(R.string.phrase_open_editor)) }
                                }
                                is SchedulePhrase.Focus -> {
                                    Text(stringResource(R.string.focus_minutes, result.minutes), style = MaterialTheme.typography.titleLarge)
                                    Text(stringResource(R.string.phrase_focus_hint))
                                    Button(onClick = { focus.start(result.minutes) { id -> startActivity(FocusActivity.intent(this@PhraseInputActivity, id)); finish() } },
                                        enabled = !state.busy, modifier = Modifier.heightIn(min = LayoutTokens.MinTouchTarget)) { Text(stringResource(R.string.phrase_start_focus)) }
                                }
                            }
                            state.error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Text(stringResource(R.string.phrase_examples_title), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.phrase_examples), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
