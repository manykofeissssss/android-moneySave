package com.example.billkeeper.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.billkeeper.data.local.entity.BillItem
import com.example.billkeeper.data.local.entity.IncomeItem
import com.example.billkeeper.data.model.formatCurrency
import com.example.billkeeper.ui.theme.CATEGORY_COLORS
import com.example.billkeeper.viewmodel.DailyLedgerViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale

private sealed interface DailyEntryItem {
    val timestamp: Long

    data class Expense(val bill: BillItem) : DailyEntryItem {
        override val timestamp: Long = bill.date
    }

    data class Income(val income: IncomeItem) : DailyEntryItem {
        override val timestamp: Long = income.date
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyLedgerScreen(viewModel: DailyLedgerViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy年M月d日") }
    val entries = remember(uiState.bills, uiState.incomes) {
        buildList {
            addAll(uiState.bills.map(DailyEntryItem::Expense))
            addAll(uiState.incomes.map(DailyEntryItem::Income))
        }.sortedByDescending { it.timestamp }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = viewModel::selectPreviousDay) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "前一天")
                }
                OutlinedButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(uiState.selectedDate.format(dateFormatter))
                }
                IconButton(onClick = viewModel::selectNextDay) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "后一天")
                }
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                tonalElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    DailySummary("收入", uiState.totalIncomeCents, Color(0xFF2E7D32))
                    DailySummary("支出", uiState.totalExpenseCents, Color(0xFFC62828))
                    DailySummary(
                        "结余",
                        uiState.balanceCents,
                        if (uiState.balanceCents >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                }
            }
        }

        item {
            Text(
                text = "共 ${uiState.entryCount} 笔记录",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }

        if (entries.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("当天暂无账单", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(
                items = entries,
                key = {
                    when (it) {
                        is DailyEntryItem.Expense -> "expense-${it.bill.id}"
                        is DailyEntryItem.Income -> "income-${it.income.id}"
                    }
                }
            ) { entry ->
                DailyEntryRow(entry)
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.selectedDate
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            viewModel.selectDate(
                                Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            )
                        }
                        showDatePicker = false
                    }
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun DailySummary(label: String, amountCents: Long, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(formatCurrency(amountCents), fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun DailyEntryRow(entry: DailyEntryItem) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val title: String
    val note: String
    val amount: Long
    val amountPrefix: String
    val color: Color
    val icon: ImageVector?

    when (entry) {
        is DailyEntryItem.Expense -> {
            title = entry.bill.category
            note = entry.bill.note
            amount = entry.bill.amountCents
            amountPrefix = "- "
            color = CATEGORY_COLORS[entry.bill.category] ?: Color(0xFFC62828)
            icon = null
        }
        is DailyEntryItem.Income -> {
            title = entry.income.source
            note = entry.income.note
            amount = entry.income.amountCents
            amountPrefix = "+ "
            color = Color(0xFF2E7D32)
            icon = Icons.AutoMirrored.Filled.TrendingUp
        }
    }

    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = {
            Text(
                listOf(timeFormatter.format(Date(entry.timestamp)), note)
                    .filter { it.isNotBlank() }
                    .joinToString(" · ")
            )
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = Color.White)
                } else {
                    Text(title.firstOrNull()?.toString().orEmpty(), color = Color.White)
                }
            }
        },
        trailingContent = {
            Text(
                "$amountPrefix${formatCurrency(amount)}",
                color = if (amountPrefix.startsWith("+")) Color(0xFF2E7D32) else Color(0xFFC62828),
                fontWeight = FontWeight.Bold
            )
        },
        modifier = Modifier.fillMaxWidth()
    )
}
