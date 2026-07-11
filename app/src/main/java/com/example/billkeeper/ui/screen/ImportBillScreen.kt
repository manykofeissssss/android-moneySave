package com.example.billkeeper.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.billkeeper.data.local.entity.BillItem
import com.example.billkeeper.ui.shared.BillRow
import com.example.billkeeper.ui.shared.LedgerEntryForm
import com.example.billkeeper.ui.shared.PieChart
import com.example.billkeeper.ui.shared.PieSlice
import com.example.billkeeper.ui.theme.CATEGORY_COLORS
import com.example.billkeeper.ui.theme.EXPENSE_CATEGORIES
import com.example.billkeeper.viewmodel.LedgerViewModel

@Composable
fun ImportBillTab(
    vm: LedgerViewModel,
    onEditBill: (BillItem) -> Unit,
    onDeleteBill: (BillItem) -> Unit
) {
    val bills by vm.monthlyBills.collectAsStateWithLifecycle()
    val pieSlices = remember(bills) {
        bills.groupBy { it.category }
            .map { (category, entries) ->
                PieSlice(
                    label = category,
                    valueCents = entries.sumOf { it.amountCents },
                    color = CATEGORY_COLORS[category] ?: Color.Gray
                )
            }
            .filter { it.valueCents > 0 }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            LedgerEntryForm(
                title = "添加支出",
                optionLabel = "类别",
                options = EXPENSE_CATEGORIES,
                actionColor = Color(0xFFC62828),
                onSubmit = { category, amountCents, note, date ->
                    vm.addBill(category, amountCents, note, date)
                }
            )
        }

        if (pieSlices.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    PieChart(
                        slices = pieSlices,
                        title = "本月支出分布",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        item {
            Text("本月账单  (${bills.size})", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(top = 4.dp))
        }

        if (bills.isEmpty()) {
            item { Text("还没有账单", color = Color.Gray, modifier = Modifier.padding(vertical = 16.dp)) }
        } else {
            items(bills, key = { it.id }) { bill ->
                BillRow(bill = bill, onEdit = onEditBill, onDelete = onDeleteBill)
            }
        }

        item { Spacer(Modifier.height(1.dp)) }
    }
}
