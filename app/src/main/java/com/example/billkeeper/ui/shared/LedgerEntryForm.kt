package com.example.billkeeper.ui.shared

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.billkeeper.data.model.parseYuanToCents
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerEntryForm(
    title: String,
    optionLabel: String,
    options: List<String>,
    actionColor: Color,
    allowDateSelection: Boolean = true,
    showTitle: Boolean = true,
    onInputFocus: () -> Unit = {},
    onSubmit: (option: String, amountCents: Long, note: String, date: Long) -> Unit
) {
    require(options.isNotEmpty()) { "Entry options must not be empty." }

    var selectedOption by remember(options) { mutableStateOf(options.first()) }
    var amountText by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf<String?>(null) }
    var noteText by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableLongStateOf(todayStartOfDayMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault()) }
    val selectedDateText = remember(selectedDate) {
        Instant.ofEpochMilli(selectedDate).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (showTitle) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(12.dp))
            }

            Text(optionLabel, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                        .padding(horizontal = 12.dp, vertical = 14.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(selectedOption, fontSize = 16.sp)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    options.forEach { option ->
                        DropdownMenuItem(text = { Text(option) }, onClick = {
                            selectedOption = option
                            expanded = false
                        })
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = amountText,
                onValueChange = {
                    amountText = it
                    amountError = null
                },
                label = { Text("金额") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                leadingIcon = { Text("¥") },
                isError = amountError != null,
                supportingText = { amountError?.let { Text(it) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { if (it.isFocused) onInputFocus() }
            )

            if (allowDateSelection) {
                Spacer(Modifier.height(8.dp))
                Text("日期", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                        .clickable { showDatePicker = true }
                        .padding(horizontal = 12.dp, vertical = 14.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(selectedDateText, fontSize = 16.sp)
                        }
                        Icon(Icons.Default.Edit, contentDescription = "选择日期", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text("备注（可选）") },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { if (it.isFocused) onInputFocus() }
            )

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    val amountCents = parseYuanToCents(amountText)
                    if (amountCents == null) {
                        amountError = "请输入大于 0 的有效金额"
                    } else {
                        val entryDate = if (allowDateSelection) {
                            selectedDate.withTimeFrom(System.currentTimeMillis())
                        } else {
                            System.currentTimeMillis()
                        }
                        onSubmit(selectedOption, amountCents, noteText.trim(), entryDate)
                        amountText = ""
                        noteText = ""
                        selectedDate = todayStartOfDayMillis()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = actionColor),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("记一笔")
            }
        }
    }

    if (allowDateSelection && showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate.toDatePickerUtcMillis())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDate = it.toLocalStartOfDayMillis() }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

internal fun Long.withTimeFrom(
    timeSourceMillis: Long,
    zoneId: ZoneId = ZoneId.systemDefault()
): Long {
    val date = Instant.ofEpochMilli(this)
        .atZone(zoneId)
        .toLocalDate()

    val time = Instant.ofEpochMilli(timeSourceMillis)
        .atZone(zoneId)
        .toLocalTime()

    return date.atTime(time)
        .atZone(zoneId)
        .toInstant()
        .toEpochMilli()
}

internal fun Long.toLocalStartOfDayMillis(zoneId: ZoneId = ZoneId.systemDefault()): Long {
    val localDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
    return localDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
}

internal fun Long.toDatePickerUtcMillis(zoneId: ZoneId = ZoneId.systemDefault()): Long {
    val localDate = Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()
    return localDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}

private fun todayStartOfDayMillis(): Long =
    LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
