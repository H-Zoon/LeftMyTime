package com.devidea.timeleft.stories

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.preference.PreferenceManager
import com.devidea.timeleft.R
import com.devidea.timeleft.preferences.UserPreferences
import com.devidea.timeleft.ui.components.allTimeStories
import com.devidea.timeleft.ui.components.TimeLeftTopAppBar
import com.devidea.timeleft.ui.theme.LayoutTokens
import com.devidea.timeleft.ui.theme.Spacing
import com.devidea.timeleft.ui.theme.TimeLeftTheme

class StoryLibraryActivity : AppCompatActivity() {
    @OptIn(ExperimentalLayoutApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        setContent {
            val (library, favorites) = rememberStoryLibrary()
            var savedOnly by rememberSaveable { mutableStateOf(true) }
            val stories = allTimeStories().filter { !savedOnly || it.id in favorites }
            TimeLeftTheme(designKey = prefs.getString(UserPreferences.KEY_DESIGN, UserPreferences.DESIGN_TIME_FOCUS) ?: UserPreferences.DESIGN_TIME_FOCUS,
                themeMode = prefs.getString(UserPreferences.KEY_THEME, UserPreferences.THEME_AUTO) ?: UserPreferences.THEME_AUTO,
                paletteKey = prefs.getString(UserPreferences.KEY_COLOR_THEME, UserPreferences.COLOR_THEME_CLAY) ?: UserPreferences.COLOR_THEME_CLAY) {
                Scaffold(containerColor = MaterialTheme.colorScheme.background,
                    topBar = { TimeLeftTopAppBar(stringResource(R.string.story_library), ::finish) }) { padding ->
                    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(horizontal = LayoutTokens.ScreenHorizontal, vertical = Spacing.l),
                        verticalArrangement = Arrangement.spacedBy(Spacing.l)) {
                        item {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                                FilterChip(savedOnly, onClick = { savedOnly = true }, label = { Text(stringResource(R.string.story_saved_filter)) }, modifier = Modifier.heightIn(min = LayoutTokens.MinTouchTarget))
                                FilterChip(!savedOnly, onClick = { savedOnly = false }, label = { Text(stringResource(R.string.story_all_filter)) }, modifier = Modifier.heightIn(min = LayoutTokens.MinTouchTarget))
                            }
                        }
                        if (stories.isEmpty()) item { Text(stringResource(R.string.story_library_empty), style = MaterialTheme.typography.bodyLarge) }
                        items(stories, key = { it.id }) { story ->
                            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Text(stringResource(story.durationRes), style = MaterialTheme.typography.titleMedium)
                                Text(stringResource(story.textRes), style = MaterialTheme.typography.bodyLarge)
                                TextButton(onClick = { library.toggle(story.id) }, modifier = Modifier.heightIn(min = LayoutTokens.MinTouchTarget)) {
                                    Text(stringResource(if (story.id in favorites) R.string.story_unsave else R.string.story_save))
                                }
                                TextButton(onClick = {
                                    try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(story.sourceUrl))) }
                                    catch (_: ActivityNotFoundException) { Toast.makeText(this@StoryLibraryActivity, R.string.time_stories_source_unavailable, Toast.LENGTH_SHORT).show() }
                                }, modifier = Modifier.heightIn(min = LayoutTokens.MinTouchTarget)) {
                                    Text(stringResource(R.string.time_stories_source, stringResource(story.sourceNameRes)))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
