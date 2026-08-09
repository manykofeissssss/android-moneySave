package com.example.billkeeper.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.billkeeper.data.local.entity.RecurringEntry
import com.example.billkeeper.data.model.centsToYuanText
import com.example.billkeeper.data.model.formatCurrency
import com.example.billkeeper.data.model.parseYuanToCents
import com.example.billkeeper.domain.recurring.RecurringEntryType
import com.example.billkeeper.domain.recurring.RecurringFrequency
import com.example.billkeeper.domain.recurring.RecurringSchedule
import com.example.billkeeper.domain.recurring.occurrenceOnOrAfter
import com.example.billkeeper.ui.theme.EXPENSE_CATEGORIES
import com.example.billkeeper.ui.theme.INCOME_SOURCES
import com.example.billkeeper.viewmodel.RecurringEntryDraft
import com.example.billkeeper.viewmodel.RecurringEntryViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun RecurringEntryScreen(vm: RecurringEntryViewModel) {
    val entries by vm.entries.collectAsStateWithLifecycle()
    var editingEntry by remember { mutableStateOf<RecurringEntry?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var deletingEntry by remember { mutableStateOf<RecurringEntry?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "周期计划",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Row {
                IconButton(onClick = vm::runDueEntriesNow) {
                    Icon(Icons.Default.Refresh, contentDescription = "立即检查到期账目")
                }
                Button(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("新增")
                }
            }
        }

        if (entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Repeat,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Color(0xFF78909C)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("暂无周期记账", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 20.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(entries, key = { it.id }) { entry ->
                    RecurringEntryCard(
                        entry = entry,
                        onEnabledChange = { vm.setEnabled(entry, it) },
                        onEdit = { editingEntry = entry },
                        onDelete = { deletingEntry = entry }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        RecurringEntryDialog(
            entry = null,
            onSave = vm::save,
            onDismiss = { showCreateDialog = false }
        )
    }
    editingEntry?.let { entry ->
        RecurringEntryDialog(
            entry = entry,
            onSave = vm::save,
            onDismiss = { editingEntry = null }
        )
    }
    deletingEntry?.let { entry ->
        AlertDialog(
            onDismissRequest = { deletingEntry = null },
            title = { Text("删除周期记账") },
            text = { Text("确定删除“${entry.categoryOrSource}”的周期计划吗？") },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete(entry)
                    deletingEntry = null
                }) {
                    Text("删除", color = Color(0xFFC62828))
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingEntry = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun RecurringEntryCard(
    entry: RecurringEntry,
    onEnabledChange: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isExpense = entry.entryType == RecurringEntryType.EXPENSE
    val accentColor = if (isExpense) Color(0xFFC62828) else Color(0xFF2E7D32)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isExpense) {
                    Icons.AutoMirrored.Filled.TrendingDown
                } else {
                    Icons.AutoMirrored.Filled.TrendingUp
                },
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = entry.categoryOrSource,
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = (if (isExpense) "- " else "+ ") + formatCurrency(entry.amountCents),
                        modifier = Modifier.widthIn(max = 140.dp),
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    text = "${entry.scheduleLabel()} · 下次 ${entry.nextRunLabel()}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
                if (entry.note.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(entry.note, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(checked = entry.enabled, onCheckedChange = onEnabledChange)
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "编辑周期记账")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "删除周期记账",
                            tint = Color(0xFFC62828)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurringEntryDialog(
    entry: RecurringEntry?,
    onSave: (RecurringEntryDraft) -> Unit,
    onDismiss: () -> Unit
) {
    var entryType by remember(entry?.id) {
        mutableStateOf(entry?.entryType ?: RecurringEntryType.EXPENSE)
    }
    var frequency by remember(entry?.id) {
        mutableStateOf(entry?.frequency ?: RecurringFrequency.MONTHLY)
    }
    var categoryOrSource by remember(entry?.id, entryType) {
        mutableStateOf(
            entry?.categoryOrSource?.takeIf { entry.entryType == entryType }
                ?: optionsFor(entryType).first()
        )
    }
    var amountText by remember(entry?.id) {
        mutableStateOf(entry?.let { centsToYuanText(it.amountCents) }.orEmpty())
    }
    var amountError by remember(entry?.id) { mutableStateOf<String?>(null) }
    var note by remember(entry?.id) { mutableStateOf(entry?.note.orEmpty()) }
    var selectedDay by remember(entry?.id, frequency) {
        mutableIntStateOf(
            when (frequency) {
                RecurringFrequency.WEEKLY -> entry?.dayOfWeek ?: LocalDate.now().dayOfWeek.value
                RecurringFrequency.MONTHLY -> entry?.dayOfMonth ?: LocalDate.now().dayOfMonth
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (entry == null) "新增周期记账" else "编辑周期记账") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EntryTypeSelector(
                    selected = entryType,
                    onSelected = {
                        entryType = it
                        categoryOrSource = optionsFor(it).first()
                    }
                )
                SelectionDropdown(
                    label = if (entryType == RecurringEntryType.EXPENSE) "支出分类" else "收入来源",
                    value = categoryOrSource,
                    options = optionsFor(entryType),
                    onSelected = { categoryOrSource = it }
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        amountError = null
                    },
                    label = { Text("金额") },
                    leadingIcon = { Text("¥") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = amountError != null,
                    supportingText = { amountError?.let { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                FrequencySelector(selected = frequency, onSelected = { frequency = it })
                SelectionDropdown(
                    label = if (frequency == RecurringFrequency.WEEKLY) "每周执行日" else "每月执行日",
                    value = selectedDayLabel(frequency, selectedDay),
                    options = dayOptions(frequency).map { it.second },
                    onSelected = { label ->
                        selectedDay = dayOptions(frequency).first { it.second == label }.first
                    }
                )
                Text(
                    text = "首次执行：${firstRunLabel(frequency, selectedDay)}",
                    color = Color(0xFF455A64),
                    fontSize = 13.sp
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注（可选）") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amountCents = parseYuanToCents(amountText)
                if (amountCents == null) {
                    amountError = "请输入大于 0 的有效金额"
                } else {
                    onSave(
                        RecurringEntryDraft(
                            id = entry?.id ?: 0,
                            entryType = entryType,
                            categoryOrSource = categoryOrSource,
                            amountCents = amountCents,
                            note = note,
                            frequency = frequency,
                            selectedDay = selectedDay
                        )
                    )
                    onDismiss()
                }
            }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryTypeSelector(
    selected: RecurringEntryType,
    onSelected: (RecurringEntryType) -> Unit
) {
    val options = listOf(RecurringEntryType.EXPENSE to "支出", RecurringEntryType.INCOME to "收入")
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (type, label) ->
            SegmentedButton(
                selected = selected == type,
                onClick = { onSelected(type) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size)
            ) {
                Text(label)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FrequencySelector(
    selected: RecurringFrequency,
    onSelected: (RecurringFrequency) -> Unit
) {
    val options = listOf(RecurringFrequency.WEEKLY to "每周", RecurringFrequency.MONTHLY to "每月")
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (frequency, label) ->
            SegmentedButton(
                selected = selected == frequency,
                onClick = { onSelected(frequency) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size)
            ) {
                Text(label)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionDropdown(
    label: String,
    value: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun optionsFor(type: RecurringEntryType): List<String> = when (type) {
    RecurringEntryType.EXPENSE -> EXPENSE_CATEGORIES
    RecurringEntryType.INCOME -> INCOME_SOURCES
}

private fun dayOptions(frequency: RecurringFrequency): List<Pair<Int, String>> = when (frequency) {
    RecurringFrequency.WEEKLY -> listOf(
        1 to "星期一",
        2 to "星期二",
        3 to "星期三",
        4 to "星期四",
        5 to "星期五",
        6 to "星期六",
        7 to "星期日"
    )

    RecurringFrequency.MONTHLY -> (1..31).map { it to "每月${it}日" }
}

private fun selectedDayLabel(frequency: RecurringFrequency, selectedDay: Int): String =
    dayOptions(frequency).first { it.first == selectedDay }.second

private fun firstRunLabel(frequency: RecurringFrequency, selectedDay: Int): String {
    val schedule = when (frequency) {
        RecurringFrequency.WEEKLY -> RecurringSchedule.Weekly(java.time.DayOfWeek.of(selectedDay))
        RecurringFrequency.MONTHLY -> RecurringSchedule.Monthly(selectedDay)
    }
    return occurrenceOnOrAfter(schedule, LocalDate.now()).format(DATE_FORMATTER)
}

private fun RecurringEntry.scheduleLabel(): String = when (frequency) {
    RecurringFrequency.WEEKLY -> selectedDayLabel(frequency, requireNotNull(dayOfWeek))
    RecurringFrequency.MONTHLY -> selectedDayLabel(frequency, requireNotNull(dayOfMonth))
}

private fun RecurringEntry.nextRunLabel(): String =
    Instant.ofEpochMilli(nextRunAt)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(DATE_FORMATTER)

private val DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE
