package com.example.billkeeper

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ExpenseSummaryTab(vm: LedgerViewModel) {
    val monthlyTotalExp by vm.monthlyTotalExpense.collectAsState(initial = 0.0)
    val monthlyTotalInc by vm.monthlyTotalIncome.collectAsState(initial = 0.0)
    val monthlyCatSummary by vm.monthlyCategorySummary.collectAsState(initial = emptyList())
    val monthlyBills by vm.monthlyBills.collectAsState(initial = emptyList())
    val prevMonthExp by vm.prevMonthTotalExpense.collectAsState(initial = 0.0)
    val monthLabel by vm.monthLabel.collectAsState()

    // 环比变化
    val momChange: Double = if (prevMonthExp > 0) monthlyTotalExp - prevMonthExp else 0.0
    val momPercent: Double = if (prevMonthExp > 0) (momChange / prevMonthExp) * 100 else 0.0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── 月份选择器 ──
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
                        monthLabel,
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

        // ── 月度概览卡片 ──
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
                        "¥ %.2f".format(monthlyTotalExp),
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            "共 ${monthlyBills.size} 笔",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        )
                        if (monthlyTotalInc > 0) {
                            Text(
                                "  |  收入 ¥%.2f".format(monthlyTotalInc),
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 13.sp
                            )
                        }
                    }
                    // 结余
                    Text(
                        "结余 ¥%.2f".format(monthlyTotalInc - monthlyTotalExp),
                        color = if (monthlyTotalInc - monthlyTotalExp >= 0)
                            Color(0xFFA5D6A7) else Color(0xFFEF9A9A),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        // ── 环比对比条 ──
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
                            val changeColor = if (momChange > 0) Color(0xFFC62828) else if (momChange < 0) Color(0xFF2E7D32) else Color.Gray
                            Text(
                                "$arrow ¥%.2f".format(kotlin.math.abs(momChange)),
                                color = changeColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                " (%.1f%%)".format(kotlin.math.abs(momPercent)),
                                color = changeColor.copy(alpha = 0.7f),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // ── 分类汇总标题 ──
        item {
            Text("按分类汇总", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(top = 4.dp))
        }

        if (monthlyCatSummary.isEmpty()) {
            item { Text("暂无支出记录", color = Color.Gray, modifier = Modifier.padding(vertical = 20.dp)) }
        } else {
            items(monthlyCatSummary) { cat -> CategoryRow(cat) }
        }

        // ── 回到本月按钮 ──
        item {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { vm.jumpToCurrentMonth() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("回到本月")
            }
        }
    }
}
