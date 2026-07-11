package com.example.billkeeper.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.billkeeper.data.model.formatCurrency
import com.example.billkeeper.ui.shared.CategoryRow
import com.example.billkeeper.viewmodel.LedgerViewModel
import kotlin.math.abs

@Composable
fun ExpenseSummaryTab(vm: LedgerViewModel) {
    val uiState by vm.monthlyUiState.collectAsStateWithLifecycle()
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
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { vm.goToPreviousMonth() }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "上月")
                    }
                    Text(
                        uiState.monthLabel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF1B5E20)
                    )
                    IconButton(onClick = { vm.goToNextMonth() }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "下月")
                    }
                }
            }
        }

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
            Text("按分类汇总", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(top = 4.dp))
        }

        if (uiState.categorySummary.isEmpty()) {
            item { Text("暂无支出记录", color = Color.Gray, modifier = Modifier.padding(vertical = 20.dp)) }
        } else {
            items(uiState.categorySummary, key = { it.category }) { cat -> CategoryRow(cat) }
        }

        item {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { vm.jumpToCurrentMonth() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text("回到本月")
            }
        }
    }
}
