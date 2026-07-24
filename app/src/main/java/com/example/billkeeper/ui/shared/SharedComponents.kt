package com.example.billkeeper.ui.shared

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.billkeeper.data.local.entity.BillItem
import com.example.billkeeper.data.local.entity.IncomeItem
import com.example.billkeeper.data.model.CategorySummary
import com.example.billkeeper.data.model.centsToYuanText
import com.example.billkeeper.data.model.formatCurrency
import com.example.billkeeper.data.model.parseYuanToCents
import com.example.billkeeper.ui.theme.CATEGORY_COLORS
import com.example.billkeeper.ui.theme.EXPENSE_CATEGORIES
import com.example.billkeeper.ui.theme.INCOME_SOURCES
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BottomSummaryItem(label: String, amountCents: Long, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Text(formatCurrency(amountCents), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun CategoryRow(cat: CategorySummary) {
    val color = CATEGORY_COLORS[cat.category] ?: Color.Gray
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).background(color, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(cat.category.first().toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Spacer(Modifier.width(12.dp))
                Text(cat.category, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
            Text(formatCurrency(cat.totalCents), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
        }
    }
}

@Composable
fun BillRow(
    bill: BillItem,
    onEdit: (BillItem) -> Unit,
    onDelete: (BillItem) -> Unit
) {
    val color = CATEGORY_COLORS[bill.category] ?: Color.Gray
    val df = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(36.dp).background(color, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Text(bill.category.first().toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(bill.category, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                if (bill.note.isNotBlank()) Text(bill.note, fontSize = 12.sp, color = Color.Gray)
                Text(df.format(Date(bill.date)), fontSize = 11.sp, color = Color.LightGray)
            }
            Text("- ${formatCurrency(bill.amountCents)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = { onEdit(bill) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "编辑", tint = Color.Gray, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = { onDelete(bill) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "删除", tint = Color(0xFFC62828), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun IncomeRow(
    income: IncomeItem,
    onEdit: (IncomeItem) -> Unit,
    onDelete: (IncomeItem) -> Unit
) {
    val df = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(36.dp).background(Color(0xFF2E7D32), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(income.source, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                if (income.note.isNotBlank()) Text(income.note, fontSize = 12.sp, color = Color.Gray)
                Text(df.format(Date(income.date)), fontSize = 11.sp, color = Color.LightGray)
            }
            Text("+ ${formatCurrency(income.amountCents)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = { onEdit(income) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "编辑", tint = Color.Gray, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = { onDelete(income) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "删除", tint = Color(0xFFC62828), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBillDialog(
    bill: BillItem,
    onSave: (BillItem) -> Unit,
    onDismiss: () -> Unit
) {
    var category by remember { mutableStateOf(bill.category) }
    var amountText by remember { mutableStateOf(centsToYuanText(bill.amountCents)) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var noteText by remember { mutableStateOf(bill.note) }
    var selectedDate by remember { mutableLongStateOf(bill.date) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑支出") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("类别") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        EXPENSE_CATEGORIES.forEach { cat ->
                            DropdownMenuItem(text = { Text(cat) }, onClick = { category = cat; expanded = false })
                        }
                    }
                }
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
                    modifier = Modifier.fillMaxWidth()
                )
                EditDateField(
                    selectedDate = selectedDate,
                    onDateSelected = { selectedDate = it }
                )
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
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
                        bill.copy(
                            category = category,
                            amountCents = amountCents,
                            date = selectedDate,
                            note = noteText
                        )
                    )
                }
            }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditIncomeDialog(
    income: IncomeItem,
    onSave: (IncomeItem) -> Unit,
    onDismiss: () -> Unit
) {
    var source by remember { mutableStateOf(income.source) }
    var amountText by remember { mutableStateOf(centsToYuanText(income.amountCents)) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var noteText by remember { mutableStateOf(income.note) }
    var selectedDate by remember { mutableLongStateOf(income.date) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑收入") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = source,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("来源") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        INCOME_SOURCES.forEach { src ->
                            DropdownMenuItem(text = { Text(src) }, onClick = { source = src; expanded = false })
                        }
                    }
                }
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
                    modifier = Modifier.fillMaxWidth()
                )
                EditDateField(
                    selectedDate = selectedDate,
                    onDateSelected = { selectedDate = it }
                )
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
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
                        income.copy(
                            source = source,
                            amountCents = amountCents,
                            date = selectedDate,
                            note = noteText
                        )
                    )
                }
            }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditDateField(
    selectedDate: Long,
    onDateSelected: (Long) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    Column {
        Text(
            text = "日期",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                .clickable { showDatePicker = true }
                .padding(horizontal = 12.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(dateFormatter.format(Date(selectedDate)), fontSize = 16.sp)
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = "选择日期",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.toDatePickerUtcMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        onDateSelected(it.toLocalStartOfDayMillis())
                    }
                    showDatePicker = false
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

data class PieSlice(val label: String, val valueCents: Long, val color: Color)

@Composable
fun PieChart(
    slices: List<PieSlice>,
    modifier: Modifier = Modifier,
    title: String = ""
) {
    val totalCents = slices.sumOf { it.valueCents }
    if (totalCents <= 0 || slices.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("暂无数据", color = Color.Gray, fontSize = 14.sp)
        }
        return
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (title.isNotEmpty()) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))
        }

        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasSize = this.size
                val canvasCenter = this.center
                var startAngle = -90f
                slices.forEach { slice ->
                    val sweep = (slice.valueCents.toDouble() / totalCents * 360).toFloat()
                    drawArc(
                        color = slice.color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = true,
                        size = canvasSize
                    )
                    startAngle += sweep
                }
                drawCircle(
                    color = Color.White,
                    radius = canvasSize.minDimension * 0.32f,
                    center = canvasCenter
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(formatCurrency(totalCents), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
                Text("合计", fontSize = 11.sp, color = Color.Gray)
            }
        }

        Spacer(Modifier.height(12.dp))

        slices.sortedByDescending { it.valueCents }.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { slice ->
                    val pct = "%.1f%%".format(slice.valueCents.toDouble() / totalCents * 100)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(10.dp)
                                .background(slice.color, RoundedCornerShape(2.dp))
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(slice.label, fontSize = 12.sp, color = Color.DarkGray, modifier = Modifier.weight(1f))
                        Text(pct, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
                    }
                }
                if (row.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
