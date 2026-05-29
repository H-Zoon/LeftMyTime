package com.devidea.timeleft.ui.editor

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
import com.devidea.timeleft.ui.theme.Spacing
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

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
    defaultDateReminderOffset: Int,
    defaultTimeReminderOffset: Int,
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

    fun defaultReminderFor(type: ItemType): Int = when (type) {
        ItemType.Date -> defaultDateReminderOffset
        ItemType.Time -> defaultTimeReminderOffset
    }

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
            if (initialItem == null) {
                reminderOffsetDays = defaultReminderFor(initialType)
            } else {
                val item = initialItem
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
                .padding(horizontal = Spacing.xl, vertical = Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.l)
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
                            reminderOffsetDays = defaultReminderFor(type)
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

                ReminderFields(
                    reminderOffsetDays = reminderOffsetDays,
                    reminderType = selectedType,
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

                AppearanceFields(
                    category = category,
                    colorKey = colorKey,
                    iconKey = iconKey,
                    onCategoryChange = { category = it },
                    onColorKeyChange = { colorKey = it },
                    onIconKeyChange = { iconKey = it }
                )

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
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
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

@Composable
private fun TimeRangeFields(
    startTimeValue: String,
    endTimeValue: String,
    onStartTimeChange: (String) -> Unit,
    onEndTimeChange: (String) -> Unit
) {
    val startTime = parseTime(startTimeValue) ?: LocalTime.now()
    val endTime = parseTime(endTimeValue) ?: startTime.plusHours(1)

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
        TimeRangeDial(
            startTime = startTime,
            endTime = endTime,
            onRangeChange = { start, end ->
                onStartTimeChange(formatStorageTime(start))
                onEndTimeChange(formatStorageTime(end))
            },
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
            TimePickerField(
                label = stringResource(R.string.editor_start_time),
                value = startTimeValue,
                onValueChange = onStartTimeChange,
                modifier = Modifier.weight(1f)
            )
            TimePickerField(
                label = stringResource(R.string.editor_end_time),
                value = endTimeValue,
                onValueChange = onEndTimeChange,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DateRangeFields(
    startDateValue: String,
    endDateValue: String,
    onStartDateChange: (String) -> Unit,
    onEndDateChange: (String) -> Unit
) {
    val startDate = parseDate(startDateValue)
    val endDate = parseDate(endDateValue)

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
        if (startDate != null && endDate != null && !endDate.isBefore(startDate)) {
            DateRangePreview(startDate = startDate, endDate = endDate)
        }
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
private fun DateRangePreview(
    startDate: LocalDate,
    endDate: LocalDate,
) {
    val today = LocalDate.now()
    val displayFormatter = DateTimeFormatter.ofPattern(stringResource(R.string.pattern_display_date))
    val daysLeft = ChronoUnit.DAYS.between(today, endDate).toInt()
    val daysSpan = (ChronoUnit.DAYS.between(startDate, endDate).toInt()).coerceAtLeast(0)
    val daysFromStart = ChronoUnit.DAYS.between(startDate, today).toInt()
    val markerProgress = when {
        daysSpan == 0 -> if (today.isBefore(startDate)) 0f else 1f
        else -> (daysFromStart.toFloat() / daysSpan).coerceIn(0f, 1f)
    }
    val showMarker = !today.isBefore(startDate) && !today.isAfter(endDate)

    val ddayText = when {
        daysLeft > 0 -> stringResource(R.string.home_dday_before, daysLeft)
        daysLeft == 0 -> stringResource(R.string.home_dday_today)
        else -> stringResource(R.string.home_dday_after, -daysLeft)
    }
    val accent = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = accent.copy(alpha = 0.08f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.m),
            verticalArrangement = Arrangement.spacedBy(Spacing.m)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = ddayText,
                    style = MaterialTheme.typography.displaySmall,
                    color = accent,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.editor_date_range_days, daysSpan),
                    style = MaterialTheme.typography.labelLarge,
                    color = onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = startDate.format(displayFormatter),
                    style = MaterialTheme.typography.bodyMedium,
                    color = onSurfaceVariant
                )
                Text(
                    text = endDate.format(displayFormatter),
                    style = MaterialTheme.typography.bodyMedium,
                    color = onSurfaceVariant
                )
            }
            DateRangeBar(
                progress = markerProgress,
                showMarker = showMarker,
                accent = accent,
                trackColor = trackColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
            )
        }
    }
}

@Composable
private fun DateRangeBar(
    progress: Float,
    showMarker: Boolean,
    accent: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val barHeightPx = 6.dp.toPx()
        val endcapRadiusPx = 5.dp.toPx()
        val markerRadiusPx = 6.dp.toPx()
        val centerY = size.height / 2f
        val startX = endcapRadiusPx
        val endX = size.width - endcapRadiusPx
        val trackWidth = endX - startX

        drawLine(
            color = trackColor,
            start = Offset(startX, centerY),
            end = Offset(endX, centerY),
            strokeWidth = barHeightPx,
            cap = StrokeCap.Round
        )
        drawCircle(
            color = accent,
            radius = endcapRadiusPx,
            center = Offset(startX, centerY)
        )
        drawCircle(
            color = accent,
            radius = endcapRadiusPx,
            center = Offset(endX, centerY)
        )
        if (showMarker) {
            val markerX = startX + trackWidth * progress.coerceIn(0f, 1f)
            drawCircle(
                color = accent.copy(alpha = 0.22f),
                radius = markerRadiusPx + 3.dp.toPx(),
                center = Offset(markerX, centerY)
            )
            drawCircle(
                color = accent,
                radius = markerRadiusPx,
                center = Offset(markerX, centerY)
            )
        }
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
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val selectedTime = parseTime(value) ?: LocalTime.now()
    val displayFormatter = DateTimeFormatter.ofPattern(stringResource(R.string.pattern_display_time))

    PickerCard(
        label = label,
        value = selectedTime.format(displayFormatter),
        modifier = modifier,
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.m)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
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
private fun ReminderFields(
    reminderOffsetDays: Int,
    reminderType: ItemType,
    showReminderPermissionMessage: Boolean,
    onReminderChange: (Int) -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(Spacing.m),
            verticalArrangement = Arrangement.spacedBy(Spacing.m)
        ) {
            Text(
                text = stringResource(R.string.editor_reminder),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                verticalArrangement = Arrangement.spacedBy(Spacing.s)
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
private fun AppearanceFields(
    category: String,
    colorKey: String,
    iconKey: String,
    onCategoryChange: (String) -> Unit,
    onColorKeyChange: (String) -> Unit,
    onIconKeyChange: (String) -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(Spacing.m),
            verticalArrangement = Arrangement.spacedBy(Spacing.m)
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
            ColorSwatchRow(
                selectedKey = colorKey,
                onSelect = onColorKeyChange
            )
            Text(
                text = stringResource(R.string.editor_icon),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconPickerGrid(
                selectedKey = iconKey,
                onSelect = onIconKeyChange
            )
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ColorSwatchRow(
    selectedKey: String,
    onSelect: (String) -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        verticalArrangement = Arrangement.spacedBy(Spacing.s)
    ) {
        ItemVisuals.colorKeys.forEach { key ->
            val color = if (key == ItemVisuals.AUTO_COLOR_KEY) primary else Color(ItemVisuals.colorInt(key))
            val selected = key == selectedKey
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable { onSelect(key) },
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .border(
                                width = 2.dp,
                                color = color,
                                shape = CircleShape
                            )
                    )
                }
                Box(
                    modifier = Modifier
                        .size(if (selected) 26.dp else 30.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun IconPickerGrid(
    selectedKey: String,
    onSelect: (String) -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val outline = MaterialTheme.colorScheme.outline
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        verticalArrangement = Arrangement.spacedBy(Spacing.s)
    ) {
        ItemVisuals.iconKeys.forEach { key ->
            val selected = key == selectedKey
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(
                        if (selected) primary.copy(alpha = 0.12f) else Color.Transparent
                    )
                    .border(
                        width = 1.dp,
                        color = if (selected) primary else outline.copy(alpha = 0.4f),
                        shape = MaterialTheme.shapes.extraSmall
                    )
                    .clickable { onSelect(key) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = itemIconVector(key),
                    contentDescription = stringResource(ItemVisuals.iconNameRes(key)),
                    tint = if (selected) primary else onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun RepeatFields(
    repeatFlag: RecurrenceMode,
    repeatRateText: String,
    onRepeatFlagChange: (RecurrenceMode) -> Unit,
    onRepeatRateChange: (String) -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(Spacing.m),
            verticalArrangement = Arrangement.spacedBy(Spacing.m)
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
        Spacer(modifier = Modifier.width(Spacing.xs))
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
