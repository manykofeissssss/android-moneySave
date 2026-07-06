package com.example.billkeeper

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

// ═══════════════════════════════ 底部汇总 ═══════════════════════════════

@Composable
fun BottomSummaryItem(label: String, amount: Double, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Text("¥%.2f".format(amount), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

// ═══════════════════════════════ Tab 0: 支出总览 ═══════════════════════════════

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
            Text("¥ %.2f".format(cat.total), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
        }
    }
}

// ═══════════════════════════════ 账单行（含编辑/删除） ═══════════════════════════════

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
            Text("- ¥%.2f".format(bill.amount), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
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

// ═══════════════════════════════ 收入行（含编辑/删除） ═══════════════════════════════

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
                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(income.source, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                if (income.note.isNotBlank()) Text(income.note, fontSize = 12.sp, color = Color.Gray)
                Text(df.format(Date(income.date)), fontSize = 11.sp, color = Color.LightGray)
            }
            Text("+ ¥%.2f".format(income.amount), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
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

// ═══════════════════════════════ 删除确认弹窗 ═══════════════════════════════

@Composable
fun DeleteConfirmDialog(
    title: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFC62828)) },
        title = { Text("确认删除") },
        text = { Text("确定要删除「$title」吗？此操作不可撤销。") },
        confirmButton = {
            TextButton(onClick = onConfirm, colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFC62828))) {
                Text("删除")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

// ═══════════════════════════════ 编辑账单弹窗 ═══════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBillDialog(
    bill: BillItem,
    onSave: (BillItem) -> Unit,
    onDismiss: () -> Unit
) {
    var category by remember { mutableStateOf(bill.category) }
    var amountText by remember { mutableStateOf(bill.amount.toString()) }
    var noteText by remember { mutableStateOf(bill.note) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑支出") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = category, onValueChange = {}, readOnly = true,
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
                    value = amountText, onValueChange = { amountText = it },
                    label = { Text("金额") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    leadingIcon = { Text("¥") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = noteText, onValueChange = { noteText = it },
                    label = { Text("备注（可选）") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amt = amountText.toDoubleOrNull()
                if (amt != null && amt > 0) {
                    onSave(bill.copy(category = category, amount = amt, note = noteText))
                }
            }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

// ═══════════════════════════════ 编辑收入弹窗 ═══════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditIncomeDialog(
    income: IncomeItem,
    onSave: (IncomeItem) -> Unit,
    onDismiss: () -> Unit
) {
    var source by remember { mutableStateOf(income.source) }
    var amountText by remember { mutableStateOf(income.amount.toString()) }
    var noteText by remember { mutableStateOf(income.note) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑收入") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = source, onValueChange = {}, readOnly = true,
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
                    value = amountText, onValueChange = { amountText = it },
                    label = { Text("金额") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    leadingIcon = { Text("¥") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = noteText, onValueChange = { noteText = it },
                    label = { Text("备注（可选）") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amt = amountText.toDoubleOrNull()
                if (amt != null && amt > 0) {
                    onSave(income.copy(source = source, amount = amt, note = noteText))
                }
            }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
