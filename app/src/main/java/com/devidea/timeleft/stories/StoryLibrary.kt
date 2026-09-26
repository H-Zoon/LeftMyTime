package com.devidea.timeleft.stories

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.devidea.timeleft.ui.components.TimeStory

/** Stores only reviewed story IDs and display history, never schedule names or dates. */
class StoryLibrary(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("story_library", Context.MODE_PRIVATE)
    fun favorites(): Set<String> = prefs.getStringSet("favorites", emptySet()).orEmpty().toSet()
    fun seenAt(id: String): Long = prefs.getLong("seen_$id", 0)
    fun markSeen(id: String) { prefs.edit().putLong("seen_$id", System.currentTimeMillis()).apply() }
    fun toggle(id: String) {
        val next = favorites().toMutableSet()
        if (!next.add(id)) next.remove(id)
        prefs.edit().putStringSet("favorites", next).apply()
    }
    internal fun order(stories: List<TimeStory>): List<TimeStory> = stories.sortedBy { seenAt(it.id) }
    fun observe(listener: SharedPreferences.OnSharedPreferenceChangeListener) = prefs.registerOnSharedPreferenceChangeListener(listener)
    fun stopObserving(listener: SharedPreferences.OnSharedPreferenceChangeListener) = prefs.unregisterOnSharedPreferenceChangeListener(listener)
}

@Composable
fun rememberStoryLibrary(): Pair<StoryLibrary, Set<String>> {
    val context = LocalContext.current
    val library = remember(context) { StoryLibrary(context) }
    var favorites by remember(library) { mutableStateOf(library.favorites()) }
    DisposableEffect(library) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key -> if (key == "favorites") favorites = library.favorites() }
        library.observe(listener)
        onDispose { library.stopObserving(listener) }
    }
    return library to favorites
}
