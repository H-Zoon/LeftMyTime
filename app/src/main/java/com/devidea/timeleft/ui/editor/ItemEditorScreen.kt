package com.devidea.timeleft.ui.editor

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devidea.timeleft.ItemVisuals
import com.devidea.timeleft.R
import com.devidea.timeleft.database.itemdata.ItemEntity
import com.devidea.timeleft.database.itemdata.ItemType
import com.devidea.timeleft.database.itemdata.RecurrenceMode
import com.devidea.timeleft.notification.canPostReminderNotifications
import com.devidea.timeleft.ui.itemIconVector
import com.devidea.timeleft.ui.permission.NotificationPermissionExplanationDialog
import com.devidea.timeleft.ui.permission.NotificationPermissionSettingsDialog
import com.devidea.timeleft.ui.permission.markNotificationPermissionRequested
import com.devidea.timeleft.ui.permission.shouldOpenNotificationSettings
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class ItemEditorDraft(
    val type: ItemType,
    val title: String,
    val startValue: String,
    val endValue: String,
    val updateFlag: RecurrenceMode,
    val updateRate: Int,
    val category: String,
    val colorKey: String,
    val iconKey: String,
    val reminderOffsetDays: Int
)

private val itemTypeSaver: Saver<ItemType, String> = Saver(
    save = { it.name },
    restore = { ItemType.valueOf(it) }
)

private val recurrenceModeSaver: Saver<RecurrenceMode, String> = Saver(
    save = { it.name },
    restore = { RecurrenceMode.valueOf(it) }
)

@Composable
fun ItemEditorScreen(
    initialType: ItemType,
    initialItem: ItemEntity?,
    isLoading: Boolean,
    isSaving: Boolean,
    onBack: () -> Unit,
    onSave: (ItemEditorDraft) -> Unit
) {
    val context = LocalContext.current
    var initialized by rememberSaveable { mutableStateOf(false) }
    var selectedType by rememberSaveable(stateSaver = itemTypeSaver) { mutableStateOf(initialType) }
    var title by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("") }
    var colorKey by rememberSaveable { mutableStateOf(ItemVisuals.AUTO_COLOR_KEY) }
    var iconKey by rememberSaveable { mutableStateOf(ItemVisuals.DEFAULT_ICON_KEY) }
    var reminderOffsetDays by rememberSaveable { mutableStateOf(ItemVisuals.REMINDER_DISABLED) }
    var startDateValue by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var endDateValue by rememberSaveable { mutableStateOf(LocalDate.now().plusDays(1).toString()) }
    var startTimeValue by rememberSaveable { mutableStateOf(formatStorageTime(LocalTime.now())) }
    var endTimeValue by rememberSaveable { mutableStateOf(formatStorageTime(LocalTime.now().plusHours(1))) }
    var repeatFlag by rememberSaveable(stateSaver = recurrenceModeSaver) {
        mutableStateOf(RecurrenceMode.None)
    }
    var repeatRateText by rememberSaveable { mutableStateOf("") }
    var errorRes by rememberSaveable { mutableStateOf<Int?>(null) }
    var pendingReminderOffsetDays by rememberSaveable { mutableStateOf<Int?>(null) }
    var saveAfterNotificationPermission by rememberSaveable { mutableStateOf(false) }
    var showNotificationPermissionExplanation by rememberSaveable { mutableStateOf(false) }
    var showNotificationPermissionSettings by rememberSaveable { mutableStateOf(false) }
    var notificationPermissionUnavailable by rememberSaveable { mutableStateOf(false) }

    fun submit(reminderOffset: Int = reminderOffsetDays) {
        errorRes = validateAndSave(
            selectedType = selectedType,
            title = title,
            startDateValue = startDateValue,
            endDateValue = endDateValue,
            startTimeValue = startTimeValue,
            endTimeValue = endTimeValue,
            repeatFlag = repeatFlag,
            repeatRateText = repeatRateText,
            category = category,
            colorKey = colorKey,
            iconKey = iconKey,
            reminderOffsetDays = reminderOffset,
            onSave = onSave
        )
    }

    fun cancelReminderAndContinue() {
        val shouldSave = saveAfterNotificationPermission
        reminderOffsetDays = ItemVisuals.REMINDER_DISABLED
        pendingReminderOffsetDays = null
        saveAfterNotificationPermission = false
        notificationPermissionUnavailable = true
        if (shouldSave) submit(ItemVisuals.REMINDER_DISABLED)
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val pendingOffset = pendingReminderOffsetDays
        if (granted) {
            pendingOffset?.let { reminderOffsetDays = it }
            notificationPermissionUnavailable = false
            if (saveAfterNotificationPermission) {
                submit(pendingOffset ?: reminderOffsetDays)
            }
            pendingReminderOffsetDays = null
            saveAfterNotificationPermission = false
        } else if (context.shouldOpenNotificationSettings()) {
            notificationPermissionUnavailable = true
            showNotificationPermissionSettings = true
        } else {
            cancelReminderAndContinue()
        }
    }
    val notificationSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        val pendingOffset = pendingReminderOffsetDays
        if (context.canPostReminderNotifications()) {
            pendingOffset?.let { reminderOffsetDays = it }
            notificationPermissionUnavailable = false
            if (saveAfterNotificationPermission) {
                submit(pendingOffset ?: reminderOffsetDays)
            }
            pendingReminderOffsetDays = null
            saveAfterNotificationPermission = false
        } else {
            cancelReminderAndContinue()
        }
    }

    LaunchedEffect(initialItem?.id, isLoading) {
        if (!initialized && !isLoading) {
            initialItem?.let { item ->
                selectedType = item.type
                title = item.title
                category = item.category
                colorKey = item.colorKey
                iconKey = item.iconKey
                reminderOffsetDays = if (context.canPostReminderNotifications()) {
                    item.reminderOffsetDays
                } else {
                    ItemVisuals.REMINDER_DISABLED
                }
                if (item.type == ItemType.Time) {
                    startTimeValue = item.startValue
                    endTimeValue = item.endValue
                } else {
                    startDateValue = item.startValue
                    endDateValue = item.endValue
                    repeatFlag = item.updateFlag
                    repeatRateText = if (item.updateRate > 0) item.updateRate.toString() else ""
                }
            }
            initialized = true
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            EditorHeader(
                title = stringResource(
                    if (initialItem == null) R.string.editor_add_title
                    else R.string.editor_edit_title
                ),
                onBack = onBack
            )

            if (isLoading) {
                LoadingState()
            } else {
                TypeSelector(
                    selectedType = selectedType,
                    enabled = initialItem == null,
                    onTypeSelected = { type ->
                        if (selectedType != type) {
                            reminderOffsetDays = ItemVisuals.REMINDER_DISABLED
                            notificationPermissionUnavailable = false
                        }
                        selectedType = type
                    }
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        errorRes = null
                    },
                    label = { Text(stringResource(R.string.editor_title_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                VisualFields(
                    category = category,
                    colorKey = colorKey,
                    iconKey = iconKey,
                    reminderOffsetDays = reminderOffsetDays,
                    reminderType = selectedType,
                    onCategoryChange = { category = it },
                    onColorKeyChange = { colorKey = it },
                    onIconKeyChange = { iconKey = it },
                    showReminderPermissionMessage = notificationPermissionUnavailable &&
                        !context.canPostReminderNotifications(),
                    onReminderChange = { offset ->
                        when {
                            offset == ItemVisuals.REMINDER_DISABLED -> {
                                reminderOffsetDays = offset
                                notificationPermissionUnavailable = false
                            }
                            context.canPostReminderNotifications() -> {
                                reminderOffsetDays = offset
                                notificationPermissionUnavailable = false
                            }
                            else -> {
                                pendingReminderOffsetDays = offset
                                saveAfterNotificationPermission = false
                                if (context.shouldOpenNotificationSettings()) {
                                    showNotificationPermissionSettings = true
                                } else {
                                    showNotificationPermissionExplanation = true
                                }
                            }
                        }
                    }
                )

                if (selectedType == ItemType.Time) {
                    TimeRangeFields(
                        startTimeValue = startTimeValue,
                        endTimeValue = endTimeValue,
                        onStartTimeChange = {
                            startTimeValue = it
                            errorRes = null
                        },
                        onEndTimeChange = {
                            endTimeValue = it
                            errorRes = null
                        }
                    )
                } else {
                    DateRangeFields(
                        startDateValue = startDateValue,
                        endDateValue = endDateValue,
                        onStartDateChange = {
                            startDateValue = it
                            errorRes = null
                        },
                        onEndDateChange = {
                            endDateValue = it
                            errorRes = null
                        }
                    )
                    RepeatFields(
                        repeatFlag = repeatFlag,
                        repeatRateText = repeatRateText,
                        onRepeatFlagChange = {
                            repeatFlag = it
                            errorRes = null
                        },
                        onRepeatRateChange = {
                            repeatRateText = it.filter(Char::isDigit)
                            errorRes = null
                        }
                    )
                }

                errorRes?.let {
                    Text(
                        text = stringResource(it),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Button(
                    onClick = {
                        if (reminderOffsetDays != ItemVisuals.REMINDER_DISABLED &&
                            !context.canPostReminderNotifications()
                        ) {
                            pendingReminderOffsetDays = reminderOffsetDays
                            saveAfterNotificationPermission = true
                            if (context.shouldOpenNotificationSettings()) {
                                showNotificationPermissionSettings = true
                            } else {
                                showNotificationPermissionExplanation = true
                            }
                        } else {
                            submit()
                        }
                    },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(if (isSaving) R.string.action_saving else R.string.action_save))
                }
            }
        }
    }

    if (showNotificationPermissionExplanation) {
        NotificationPermissionExplanationDialog(
            onAllow = {
                showNotificationPermissionExplanation = false
                if (context.shouldOpenNotificationSettings()) {
                    showNotificationPermissionSettings = true
                } else {
                    context.markNotificationPermissionRequested()
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            onDismiss = {
                showNotificationPermissionExplanation = false
                cancelReminderAndContinue()
            }
        )
    }

    if (showNotificationPermissionSettings) {
        NotificationPermissionSettingsDialog(
            onOpenSettings = {
                showNotificationPermissionSettings = false
                notificationSettingsLauncher.launch(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                )
            },
            onDismiss = {
                showNotificationPermissionSettings = false
                cancelReminderAndContinue()
            }
        )
    }
}

@Composable
private fun EditorHeader(
    title: String,
    onBack: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.action_back)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LoadingState() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun TypeSelector(
    selectedType: ItemType,
    enabled: Boolean,
    onTypeSelected: (ItemType) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.editor_type),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selectedType == ItemType.Time,
                onClick = { if (enabled) onTypeSelected(ItemType.Time) },
                enabled = enabled,
                label = { Text(stringResource(R.string.home_add_time_range)) },
                leadingIcon = { Icon(Icons.Filled.Schedule, contentDescription = null) }
            )
            FilterChip(
                selected = selectedType == ItemType.Date,
                onClick = { if (enabled) onTypeSelected(ItemType.Date) },
                enabled = enabled,
                label = { Text(stringResource(R.string.home_add_date)) },
                leadingIcon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) }
            )
        }
    }
}

@Composable
private fun TimeRangeFields(
    startTimeValue: String,
    endTimeValue: String,
    onStartTimeChange: (String) -> Unit,
    onEndTimeChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TimePickerField(
            label = stringResource(R.string.editor_start_time),
            value = startTimeValue,
            onValueChange = onStartTimeChange
        )
        TimePickerField(
            label = stringResource(R.string.editor_end_time),
            value = endTimeValue,
            onValueChange = onEndTimeChange
        )
    }
}

@Composable
private fun DateRangeFields(
    startDateValue: String,
    endDateValue: String,
    onStartDateChange: (String) -> Unit,
    onEndDateChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DatePickerField(
            label = stringResource(R.string.editor_start_date),
            value = startDateValue,
            onValueChange = onStartDateChange
        )
        DatePickerField(
            label = stringResource(R.string.editor_end_date),
            value = endDateValue,
            onValueChange = onEndDateChange
        )
    }
}

@Composable
private fun DatePickerField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    val context = LocalContext.current
    val selectedDate = parseDate(value) ?: LocalDate.now()
    val displayFormatter = DateTimeFormatter.ofPattern(stringResource(R.string.pattern_display_date))

    PickerCard(
        label = label,
        value = selectedDate.format(displayFormatter),
        onClick = {
            DatePickerDialog(
                context,
                { _, year, month, day ->
                    onValueChange(LocalDate.of(year, month + 1, day).toString())
                },
                selectedDate.year,
                selectedDate.monthValue - 1,
                selectedDate.dayOfMonth
            ).show()
        }
    )
}

@Composable
private fun TimePickerField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    val context = LocalContext.current
    val selectedTime = parseTime(value) ?: LocalTime.now()
    val displayFormatter = DateTimeFormatter.ofPattern(stringResource(R.string.pattern_display_time))

    PickerCard(
        label = label,
        value = selectedTime.format(displayFormatter),
        onClick = {
            TimePickerDialog(
                context,
                { _, hour, minute ->
                    onValueChange(formatStorageTime(LocalTime.of(hour, minute)))
                },
                selectedTime.hour,
                selectedTime.minute,
                false
            ).show()
        }
    )
}

@Composable
private fun PickerCard(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun VisualFields(
    category: String,
    colorKey: String,
    iconKey: String,
    reminderOffsetDays: Int,
    reminderType: ItemType,
    onCategoryChange: (String) -> Unit,
    onColorKeyChange: (String) -> Unit,
    onIconKeyChange: (String) -> Unit,
    showReminderPermissionMessage: Boolean,
    onReminderChange: (Int) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.editor_appearance),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            OutlinedTextField(
                value = category,
                onValueChange = onCategoryChange,
                label = { Text(stringResource(R.string.editor_category_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = stringResource(R.string.editor_color),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ItemVisuals.colorKeys.forEach { key ->
                    FilterChip(
                        selected = colorKey == key,
                        onClick = { onColorKeyChange(key) },
                        label = { Text(stringResource(ItemVisuals.colorNameRes(key))) },
                        leadingIcon = {
                            ColorSwatch(
                                color = if (key == ItemVisuals.AUTO_COLOR_KEY) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color(ItemVisuals.colorInt(key))
                                }
                            )
                        }
                    )
                }
            }
            Text(
                text = stringResource(R.string.editor_icon),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ItemVisuals.iconKeys.forEach { key ->
                    FilterChip(
                        selected = iconKey == key,
                        onClick = { onIconKeyChange(key) },
                        label = { Text(stringResource(ItemVisuals.iconNameRes(key))) },
                        leadingIcon = {
                            Icon(
                                imageVector = itemIconVector(key),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }
            Text(
                text = stringResource(R.string.editor_reminder),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ItemVisuals.reminderOffsets(reminderType).forEach { offset ->
                    FilterChip(
                        selected = reminderOffsetDays == offset,
                        onClick = { onReminderChange(offset) },
                        label = { Text(stringResource(ItemVisuals.reminderNameRes(reminderType, offset))) }
                    )
                }
            }
            if (showReminderPermissionMessage) {
                Text(
                    text = stringResource(R.string.editor_reminder_permission_required),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun ColorSwatch(color: Color) {
    Surface(
        modifier = Modifier.size(18.dp),
        shape = CircleShape,
        color = color,
    ) {
        Box(modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun RepeatFields(
    repeatFlag: RecurrenceMode,
    repeatRateText: String,
    onRepeatFlagChange: (RecurrenceMode) -> Unit,
    onRepeatRateChange: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.editor_repeat),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            RepeatOption(
                selected = repeatFlag == RecurrenceMode.None,
                text = stringResource(R.string.editor_repeat_none),
                onClick = { onRepeatFlagChange(RecurrenceMode.None) }
            )
            RepeatOption(
                selected = repeatFlag == RecurrenceMode.Day,
                text = stringResource(R.string.editor_repeat_every_n_days),
                onClick = { onRepeatFlagChange(RecurrenceMode.Day) }
            )
            if (repeatFlag == RecurrenceMode.Day) {
                NumberField(
                    value = repeatRateText,
                    onValueChange = onRepeatRateChange,
                    label = stringResource(R.string.editor_repeat_interval),
                    suffix = stringResource(R.string.editor_days_suffix)
                )
                Text(
                    text = stringResource(R.string.editor_repeat_help_day),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RepeatOption(
                selected = repeatFlag == RecurrenceMode.Month,
                text = stringResource(R.string.editor_repeat_every_month),
                onClick = { onRepeatFlagChange(RecurrenceMode.Month) }
            )
            if (repeatFlag == RecurrenceMode.Month) {
                NumberField(
                    value = repeatRateText,
                    onValueChange = onRepeatRateChange,
                    label = stringResource(R.string.editor_repeat_day_of_month),
                    suffix = stringResource(R.string.editor_day_of_month_suffix)
                )
                Text(
                    text = stringResource(R.string.editor_repeat_help_month),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RepeatOption(
    selected: Boolean,
    text: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    suffix: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        suffix = { Text(suffix) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
}

@StringRes
private fun validateAndSave(
    selectedType: ItemType,
    title: String,
    startDateValue: String,
    endDateValue: String,
    startTimeValue: String,
    endTimeValue: String,
    repeatFlag: RecurrenceMode,
    repeatRateText: String,
    category: String,
    colorKey: String,
    iconKey: String,
    reminderOffsetDays: Int,
    onSave: (ItemEditorDraft) -> Unit
): Int? {
    val cleanTitle = title.trim()
    if (cleanTitle.isBlank()) return R.string.editor_error_title_required

    if (selectedType == ItemType.Time) {
        val startTime = parseTime(startTimeValue) ?: return R.string.editor_error_invalid_start_time
        val endTime = parseTime(endTimeValue) ?: return R.string.editor_error_invalid_end_time
        if (!endTime.isAfter(startTime)) return R.string.editor_error_end_before_start_time

        onSave(
            ItemEditorDraft(
                type = ItemType.Time,
                title = cleanTitle,
                startValue = formatStorageTime(startTime),
                endValue = formatStorageTime(endTime),
                updateFlag = RecurrenceMode.TimeRange,
                updateRate = 0,
                category = category.trim(),
                colorKey = colorKey,
                iconKey = iconKey,
                reminderOffsetDays = reminderOffsetDays
            )
        )
        return null
    }

    val startDate = parseDate(startDateValue) ?: return R.string.editor_error_invalid_start_date
    val endDate = parseDate(endDateValue) ?: return R.string.editor_error_invalid_end_date
    if (endDate.isBefore(startDate)) return R.string.editor_error_end_before_start_date

    val updateRate = when (repeatFlag) {
        RecurrenceMode.Day, RecurrenceMode.Month -> repeatRateText.toIntOrNull()
        RecurrenceMode.None, RecurrenceMode.TimeRange -> 0
    } ?: return R.string.editor_error_repeat_required

    if (repeatFlag == RecurrenceMode.Day && updateRate < 1) {
        return R.string.editor_error_repeat_day_min
    }
    if (repeatFlag == RecurrenceMode.Month && updateRate !in 1..31) {
        return R.string.editor_error_repeat_month_range
    }

    onSave(
        ItemEditorDraft(
            type = ItemType.Date,
            title = cleanTitle,
            startValue = startDate.toString(),
            endValue = endDate.toString(),
            updateFlag = repeatFlag,
            updateRate = updateRate,
            category = category.trim(),
            colorKey = colorKey,
            iconKey = iconKey,
            reminderOffsetDays = reminderOffsetDays
        )
    )
    return null
}

private fun parseDate(value: String): LocalDate? =
    runCatching { LocalDate.parse(value, STORAGE_DATE_FORMATTER) }.getOrNull()

private fun parseTime(value: String): LocalTime? =
    runCatching { LocalTime.parse(value, STORAGE_TIME_FORMATTER) }.getOrNull()

private fun formatStorageTime(time: LocalTime): String = "${time.hour}:${time.minute}"

private val STORAGE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-M-d")
private val STORAGE_TIME_FORMATTER = DateTimeFormatter.ofPattern("H:m")
