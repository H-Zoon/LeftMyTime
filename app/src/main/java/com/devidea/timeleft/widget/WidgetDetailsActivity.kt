package com.devidea.timeleft.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.devidea.timeleft.ItemGenerate
import com.devidea.timeleft.R
import com.devidea.timeleft.activity.ItemEditorActivity
import com.devidea.timeleft.database.itemdata.ItemEntity
import com.devidea.timeleft.database.itemdata.ItemType
import com.devidea.timeleft.preferences.UserPreferences
import com.devidea.timeleft.repository.TimeLeftRepository
import com.devidea.timeleft.ui.components.TimeDetailContent
import com.devidea.timeleft.ui.components.TimeLeftTopAppBar
import com.devidea.timeleft.ui.theme.LayoutTokens
import com.devidea.timeleft.ui.theme.Spacing
import com.devidea.timeleft.ui.theme.TimeLeftTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.collect
import javax.inject.Inject

/** Opens the item actually rendered on the widget, without running auto-selection again. */
@AndroidEntryPoint
class WidgetDetailsActivity : AppCompatActivity() {
    @Inject lateinit var prefs: SharedPreferences
    @Inject lateinit var repository: TimeLeftRepository
    private var targetIntent by mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        targetIntent = intent
        setContent {
            val target = targetIntent ?: return@setContent
            val source = WidgetSource.fromPrefValue(target.getStringExtra(SOURCE).orEmpty())
            val itemId = target.getIntExtra(ITEM, -1)
            val widgetId = target.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            val personal = source == WidgetSource.Custom || source == WidgetSource.Next
            var entities by remember(target) { mutableStateOf<List<ItemEntity>?>(null) }
            var failed by remember(target) { mutableStateOf(false) }
            var retry by remember(target) { mutableIntStateOf(0) }
            LaunchedEffect(target, retry) {
                if (personal) {
                    failed = false
                    entities = null
                    try { lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { repository.items.collect { entities = it } } }
                    catch (exception: CancellationException) { throw exception }
                    catch (_: Exception) { entities = null; failed = true }
                }
            }
            // Only while this detail screen is visible; no background service or new alarm.
            var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
            LaunchedEffect(target) {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    while (isActive) { now = System.currentTimeMillis(); delay(1_000) }
                }
            }
            val generator = remember { ItemGenerate(this) }
            val itemResult = remember(source, itemId, entities, now) { runCatching {
                when (source) {
                    WidgetSource.Today -> generator.timeItem()
                    WidgetSource.Month -> generator.monthItem()
                    WidgetSource.Year -> generator.yearItem()
                    else -> entities?.firstOrNull { it.id == itemId }?.forWidgetPreview()?.let {
                        if (it.type == ItemType.Time) generator.customTimeItem(it) else generator.customMonthItem(it)
                    }
                }
            } }
            val item = itemResult.getOrNull()
            val detailFailed = failed || itemResult.isFailure
            TimeLeftTheme(
                themeMode = prefs.getString(UserPreferences.KEY_THEME, UserPreferences.THEME_AUTO) ?: UserPreferences.THEME_AUTO,
                paletteKey = prefs.getString(UserPreferences.KEY_COLOR_THEME, UserPreferences.COLOR_THEME_CLAY) ?: UserPreferences.COLOR_THEME_CLAY,
            ) {
                Scaffold(topBar = { TimeLeftTopAppBar(item?.title ?: stringResource(source.labelRes), ::finish) },
                    containerColor = MaterialTheme.colorScheme.background) { padding ->
                    Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                        .padding(horizontal = LayoutTokens.ScreenHorizontal, vertical = Spacing.l),
                        verticalArrangement = Arrangement.spacedBy(LayoutTokens.SectionGap)) {
                        if (item == null) {
                            Text(stringResource(when {
                                detailFailed -> R.string.widget_load_failed
                                entities == null && personal -> R.string.widget_loading
                                else -> R.string.widget_selected_deleted
                            }), style = MaterialTheme.typography.bodyLarge)
                            if (detailFailed) TextButton(onClick = { retry++ }, modifier = Modifier.heightIn(min = LayoutTokens.MinTouchTarget)) { Text(stringResource(R.string.widget_retry)) }
                            else if (entities != null && AppWidgetManager.getInstance(this@WidgetDetailsActivity).getAppWidgetInfo(widgetId) != null) {
                                TextButton(onClick = {
                                    startActivity(Intent(this@WidgetDetailsActivity, AppWidgetConfigure::class.java)
                                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
                                    finish()
                                }, modifier = Modifier.heightIn(min = LayoutTokens.MinTouchTarget)) { Text(stringResource(R.string.widget_choose_schedule)) }
                            }
                        } else {
                            TimeDetailContent(item, prefs.getString(UserPreferences.KEY_PROGRESS_DISPLAY, UserPreferences.PROGRESS_DISPLAY_FULL)
                                ?: UserPreferences.PROGRESS_DISPLAY_FULL)
                            if (personal) TextButton(onClick = { startActivity(ItemEditorActivity.editIntent(this@WidgetDetailsActivity, item.id)) },
                                modifier = Modifier.heightIn(min = LayoutTokens.MinTouchTarget)) {
                                Text(stringResource(R.string.card_action_edit))
                            }
                            PinWidgetButton(item, source, contentPadding = PaddingValues(vertical = Spacing.s))
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        targetIntent = intent
    }

    companion object {
        private const val SOURCE = "widget_source"
        private const val ITEM = "widget_item_id"
        internal fun createIntent(context: Context, widgetId: Int, source: WidgetSource, itemId: Int?): Intent =
            Intent(context, WidgetDetailsActivity::class.java).apply {
                data = Uri.Builder().scheme("timeleft").authority("widget").appendPath(widgetId.toString())
                    .appendPath(source.prefValue).appendPath(itemId?.toString() ?: "period").build()
                putExtra(SOURCE, source.prefValue)
                itemId?.let { putExtra(ITEM, it) }
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
    }
}
