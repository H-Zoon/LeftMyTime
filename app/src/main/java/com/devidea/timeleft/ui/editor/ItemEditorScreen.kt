package com.devidea.timeleft.ui.editor

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devidea.timeleft.activity.MainActivity
import com.devidea.timeleft.calc.TimeProgressCalculator
import com.devidea.timeleft.datadase.itemdata.ItemEntity
import com.devidea.timeleft.repository.TimeLeftRepository
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class ItemEditorDraft(
    val type: String,
    val title: String,
    val startValue: String,
    val endValue: String,
    val updateFlag: Int,
    val updateRate: Int
)

@Composable
fun ItemEditorScreen(
    initialType: String,
    initialItem: ItemEntity?,
    isLoading: Boolean,
    isSaving: Boolean,
    onBack: () -> Unit,
    onSave: (ItemEditorDraft) -> Unit
) {
    var initialized by rememberSaveable { mutableStateOf(false) }
    var selectedType by rememberSaveable { mutableStateOf(initialType) }
    var title by rememberSaveable { mutableStateOf("") }
    var startDateValue by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var endDateValue by rememberSaveable { mutableStateOf(LocalDate.now().plusDays(1).toString()) }
    var startTimeValue by rememberSaveable { mutableStateOf(formatStorageTime(LocalTime.now())) }
    var endTimeValue by rememberSaveable { mutableStateOf(formatStorageTime(LocalTime.now().plusHours(1))) }
    var repeatFlag by rememberSaveable { mutableIntStateOf(MainActivity.UPDATE_FLAG_UNABLE) }
    var repeatRateText by rememberSaveable { mutableStateOf("") }
    var errorText by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(initialItem?.id, isLoading) {
        if (!initialized && !isLoading) {
            initialItem?.let { item ->
                selectedType = item.type
                title = item.title
                if (item.type == TimeLeftRepository.TYPE_TIME) {
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
                title = if (initialItem == null) "항목 추가" else "항목 수정",
                onBack = onBack
            )

            if (isLoading) {
                LoadingState()
            } else {
                TypeSelector(
                    selectedType = selectedType,
                    enabled = initialItem == null,
                    onTypeSelected = { selectedType = it }
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        errorText = null
                    },
                    label = { Text("제목") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (selectedType == TimeLeftRepository.TYPE_TIME) {
                    TimeRangeFields(
                        startTimeValue = startTimeValue,
                        endTimeValue = endTimeValue,
                        onStartTimeChange = {
                            startTimeValue = it
                            errorText = null
                        },
                        onEndTimeChange = {
                            endTimeValue = it
                            errorText = null
                        }
                    )
                } else {
                    DateRangeFields(
                        startDateValue = startDateValue,
                        endDateValue = endDateValue,
                        onStartDateChange = {
                            startDateValue = it
                            errorText = null
                        },
                        onEndDateChange = {
                            endDateValue = it
                            errorText = null
                        }
                    )
                    RepeatFields(
                        repeatFlag = repeatFlag,
                        repeatRateText = repeatRateText,
                        onRepeatFlagChange = {
                            repeatFlag = it
                            errorText = null
                        },
                        onRepeatRateChange = {
                            repeatRateText = it.filter(Char::isDigit)
                            errorText = null
                        }
                    )
                }

                errorText?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Button(
                    onClick = {
                        errorText = validateAndSave(
                            selectedType = selectedType,
                            title = title,
                            startDateValue = startDateValue,
                            endDateValue = endDateValue,
                            startTimeValue = startTimeValue,
                            endTimeValue = endTimeValue,
                            repeatFlag = repeatFlag,
                            repeatRateText = repeatRateText,
                            onSave = onSave
                        )
                    },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isSaving) "저장 중" else "저장")
                }
            }
        }
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
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
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
    selectedType: String,
    enabled: Boolean,
    onTypeSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "유형",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selectedType == TimeLeftRepository.TYPE_TIME,
                onClick = { if (enabled) onTypeSelected(TimeLeftRepository.TYPE_TIME) },
                enabled = enabled,
                label = { Text("시간 범위") },
                leadingIcon = { Icon(Icons.Filled.Schedule, contentDescription = null) }
            )
            FilterChip(
                selected = selectedType == TimeLeftRepository.TYPE_DATE,
                onClick = { if (enabled) onTypeSelected(TimeLeftRepository.TYPE_DATE) },
                enabled = enabled,
                label = { Text("날짜") },
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
            label = "시작 시간",
            value = startTimeValue,
            onValueChange = onStartTimeChange
        )
        TimePickerField(
            label = "종료 시간",
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
            label = "시작일",
            value = startDateValue,
            onValueChange = onStartDateChange
        )
        DatePickerField(
            label = "종료일",
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

    PickerCard(
        label = label,
        value = selectedDate.format(DISPLAY_DATE_FORMATTER),
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

    PickerCard(
        label = label,
        value = selectedTime.format(DISPLAY_TIME_FORMATTER),
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
private fun RepeatFields(
    repeatFlag: Int,
    repeatRateText: String,
    onRepeatFlagChange: (Int) -> Unit,
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
                text = "반복",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            RepeatOption(
                selected = repeatFlag == MainActivity.UPDATE_FLAG_UNABLE,
                text = "반복 없음",
                onClick = { onRepeatFlagChange(MainActivity.UPDATE_FLAG_UNABLE) }
            )
            RepeatOption(
                selected = repeatFlag == TimeProgressCalculator.UPDATE_FLAG_DAY,
                text = "종료 후 지정한 일수마다 반복",
                onClick = { onRepeatFlagChange(TimeProgressCalculator.UPDATE_FLAG_DAY) }
            )
            if (repeatFlag == TimeProgressCalculator.UPDATE_FLAG_DAY) {
                NumberField(
                    value = repeatRateText,
                    onValueChange = onRepeatRateChange,
                    label = "반복 간격",
                    suffix = "일"
                )
            }
            RepeatOption(
                selected = repeatFlag == TimeProgressCalculator.UPDATE_FLAG_MONTH,
                text = "매달 지정한 날짜에 반복",
                onClick = { onRepeatFlagChange(TimeProgressCalculator.UPDATE_FLAG_MONTH) }
            )
            if (repeatFlag == TimeProgressCalculator.UPDATE_FLAG_MONTH) {
                NumberField(
                    value = repeatRateText,
                    onValueChange = onRepeatRateChange,
                    label = "매달 반복일",
                    suffix = "일"
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

private fun validateAndSave(
    selectedType: String,
    title: String,
    startDateValue: String,
    endDateValue: String,
    startTimeValue: String,
    endTimeValue: String,
    repeatFlag: Int,
    repeatRateText: String,
    onSave: (ItemEditorDraft) -> Unit
): String? {
    val cleanTitle = title.trim()
    if (cleanTitle.isBlank()) return "제목을 입력해주세요."

    if (selectedType == TimeLeftRepository.TYPE_TIME) {
        val startTime = parseTime(startTimeValue) ?: return "시작 시간을 확인해주세요."
        val endTime = parseTime(endTimeValue) ?: return "종료 시간을 확인해주세요."
        if (!endTime.isAfter(startTime)) return "종료 시간은 시작 시간보다 늦어야 합니다."

        onSave(
            ItemEditorDraft(
                type = TimeLeftRepository.TYPE_TIME,
                title = cleanTitle,
                startValue = formatStorageTime(startTime),
                endValue = formatStorageTime(endTime),
                updateFlag = MainActivity.UPDATE_FLAG_FOR_TIME,
                updateRate = 0
            )
        )
        return null
    }

    val startDate = parseDate(startDateValue) ?: return "시작일을 확인해주세요."
    val endDate = parseDate(endDateValue) ?: return "종료일을 확인해주세요."
    if (endDate.isBefore(startDate)) return "종료일은 시작일보다 빠를 수 없습니다."

    val updateRate = when (repeatFlag) {
        TimeProgressCalculator.UPDATE_FLAG_DAY,
        TimeProgressCalculator.UPDATE_FLAG_MONTH -> repeatRateText.toIntOrNull()
        else -> 0
    } ?: return "반복 값을 입력해주세요."

    if (repeatFlag == TimeProgressCalculator.UPDATE_FLAG_DAY && updateRate < 1) {
        return "반복 간격은 1일 이상이어야 합니다."
    }
    if (repeatFlag == TimeProgressCalculator.UPDATE_FLAG_MONTH && updateRate !in 1..31) {
        return "반복일은 1일부터 31일 사이여야 합니다."
    }

    onSave(
        ItemEditorDraft(
            type = TimeLeftRepository.TYPE_DATE,
            title = cleanTitle,
            startValue = startDate.toString(),
            endValue = endDate.toString(),
            updateFlag = repeatFlag,
            updateRate = updateRate
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
private val DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy년 M월 d일")
private val DISPLAY_TIME_FORMATTER = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN)
