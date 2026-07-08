package com.example.billkeeper

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BillKeeperApp(vm: LedgerViewModel) {
    val tabs = listOf("支出总览", "记录支出", "录入收入")
    val pagerState = rememberPagerState { tabs.size }
    val coroutineScope = rememberCoroutineScope()

    // 底部栏需要的全量数据
    val totalExp by vm.totalExpense.collectAsState(initial = 0.0)
    val totalInc by vm.totalIncome.collectAsState(initial = 0.0)

    // Snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        vm.snackbarMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
        }
    }

    // 删除确认状态
    var billToDelete by remember { mutableStateOf<BillItem?>(null) }
    var incomeToDelete by remember { mutableStateOf<IncomeItem?>(null) }

    // 编辑状态
    var billToEdit by remember { mutableStateOf<BillItem?>(null) }
    var incomeToEdit by remember { mutableStateOf<IncomeItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("💰 记账本", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1B5E20),
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomSummaryItem("总收入", totalInc, Color(0xFF2E7D32))
                    BottomSummaryItem("总支出", totalExp, Color(0xFFC62828))
                    BottomSummaryItem("结余", totalInc - totalExp,
                        if (totalInc - totalExp >= 0) Color(0xFF2E7D32) else Color(0xFFC62828))
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = Color.White,
                contentColor = Color(0xFF1B5E20)
            ) {
                tabs.forEachIndexed { idx, title ->
                    Tab(
                        selected = pagerState.currentPage == idx,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(idx) } },
                        text = {
                            Text(
                                title,
                                fontWeight = if (pagerState.currentPage == idx) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // HorizontalPager 保留各页 Composable 状态，切换时无需重建
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                when (page) {
                    0 -> ExpenseSummaryTab(vm)
                    1 -> ImportBillTab(vm,
                        onEditBill = { billToEdit = it },
                        onDeleteBill = { billToDelete = it }
                    )
                    2 -> AddIncomeTab(vm,
                        onEditIncome = { incomeToEdit = it },
                        onDeleteIncome = { incomeToDelete = it }
                    )
                }
            }
        }
    }

    // ── 删除确认弹窗 ──
    billToDelete?.let { bill ->
        DeleteConfirmDialog(
            title = "${bill.category} ¥${String.format("%.2f", bill.amount)}",
            onConfirm = { vm.deleteBill(bill); billToDelete = null },
            onDismiss = { billToDelete = null }
        )
    }
    incomeToDelete?.let { income ->
        DeleteConfirmDialog(
            title = "${income.source} ¥${String.format("%.2f", income.amount)}",
            onConfirm = { vm.deleteIncome(income); incomeToDelete = null },
            onDismiss = { incomeToDelete = null }
        )
    }

    // ── 编辑弹窗 ──
    billToEdit?.let { bill ->
        EditBillDialog(
            bill = bill,
            onSave = { vm.updateBill(it); billToEdit = null },
            onDismiss = { billToEdit = null }
        )
    }
    incomeToEdit?.let { income ->
        EditIncomeDialog(
            income = income,
            onSave = { vm.updateIncome(it); incomeToEdit = null },
            onDismiss = { incomeToEdit = null }
        )
    }
}
