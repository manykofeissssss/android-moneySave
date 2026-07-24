package com.example.billkeeper.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.billkeeper.data.model.formatCurrency
import com.example.billkeeper.ui.shared.CategoryRow
import com.example.billkeeper.ui.shared.LedgerEntryForm
import com.example.billkeeper.ui.theme.EXPENSE_CATEGORIES
import com.example.billkeeper.ui.theme.INCOME_SOURCES
import com.example.billkeeper.viewmodel.LedgerViewModel
import kotlin.math.abs

@Composable
fun ExpenseSummaryTab(vm: LedgerViewModel) {
    val uiState by vm.monthlyUiState.collectAsStateWithLifecycle()
    var showQuickEntry by rememberSaveable { mutableStateOf(false) }
    var categorySummaryExpanded by rememberSaveable { mutableStateOf(false) }
    val monthlyTotalExp = uiState.totalExpenseCents
    val monthlyTotalInc = uiState.totalIncomeCents
    val prevMonthExp = uiState.prevMonthExpenseCents

    val momChange = if (prevMonthExp > 0) monthlyTotalExp - prevMonthExp else 0L
    val momPercent = if (prevMonthExp > 0) (momChange.toDouble() / prevMonthExp) * 100 else 0.0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("月度支出", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                    Text(
                        formatCurrency(monthlyTotalExp),
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            "共 ${uiState.bills.size} 笔",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        )
                        if (monthlyTotalInc > 0) {
                            Text(
                                "  |  收入 ${formatCurrency(monthlyTotalInc)}",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 13.sp
                            )
                        }
                    }
                    Text(
                        "结余 ${formatCurrency(monthlyTotalInc - monthlyTotalExp)}",
                        color = if (monthlyTotalInc - monthlyTotalExp >= 0) {
                            Color(0xFFA5D6A7)
                        } else {
                            Color(0xFFEF9A9A)
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        item {
            if (prevMonthExp > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("较上月", fontSize = 14.sp, color = Color.Gray)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val arrow = if (momChange > 0) "↑" else if (momChange < 0) "↓" else "→"
                            val changeColor = if (momChange > 0) {
                                Color(0xFFC62828)
                            } else if (momChange < 0) {
                                Color(0xFF2E7D32)
                            } else {
                                Color.Gray
                            }
                            Text(
                                "$arrow ${formatCurrency(abs(momChange))}",
                                color = changeColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                " (%.1f%%)".format(abs(momPercent)),
                                color = changeColor.copy(alpha = 0.7f),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = { showQuickEntry = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("快速记一笔")
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { categorySummaryExpanded = !categorySummaryExpanded }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("分类汇总", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${uiState.categorySummary.size} 类",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                    Icon(
                        imageVector = if (categorySummaryExpanded) {
                            Icons.Default.ExpandLess
                        } else {
                            Icons.Default.ExpandMore
                        },
                        contentDescription = if (categorySummaryExpanded) "收起分类汇总" else "展开分类汇总",
                        tint = Color.Gray
                    )
                }
            }
        }

        if (categorySummaryExpanded) {
            if (uiState.categorySummary.isEmpty()) {
                item { Text("暂无支出记录", color = Color.Gray, modifier = Modifier.padding(vertical = 20.dp)) }
            } else {
                items(uiState.categorySummary, key = { it.category }) { cat -> CategoryRow(cat) }
            }
        }
    }

    if (showQuickEntry) {
        QuickEntrySheet(
            vm = vm,
            onDismiss = { showQuickEntry = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickEntrySheet(
    vm: LedgerViewModel,
    onDismiss: () -> Unit
) {
    var selectedType by remember { mutableIntStateOf(0) }
    val entryTypes = listOf("支出", "收入")

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("快速记一笔", fontWeight = FontWeight.Bold, fontSize = 20.sp)

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                entryTypes.forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = selectedType == index,
                        onClick = { selectedType = index },
                        shape = SegmentedButtonDefaults.itemShape(index, entryTypes.size),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(label)
                    }
                }
            }

            if (selectedType == 0) {
                LedgerEntryForm(
                    title = "记录支出",
                    optionLabel = "类别",
                    options = EXPENSE_CATEGORIES,
                    actionColor = Color(0xFFC62828),
                    onSubmit = { category, amountCents, note, date ->
                        vm.addBill(category, amountCents, note, date)
                        onDismiss()
                    }
                )
            } else {
                LedgerEntryForm(
                    title = "记录收入",
                    optionLabel = "来源",
                    options = INCOME_SOURCES,
                    actionColor = Color(0xFF2E7D32),
                    onSubmit = { source, amountCents, note, date ->
                        vm.addIncome(source, amountCents, note, date)
                        onDismiss()
                    }
                )
            }
        }
    }
}
