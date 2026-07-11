package com.example.billkeeper.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.billkeeper.data.local.entity.BillItem
import com.example.billkeeper.data.local.entity.IncomeItem
import com.example.billkeeper.data.model.formatCurrency
import com.example.billkeeper.ui.screen.AddIncomeTab
import com.example.billkeeper.ui.screen.ExpenseSummaryTab
import com.example.billkeeper.ui.screen.ImportBillTab
import com.example.billkeeper.ui.shared.BottomSummaryItem
import com.example.billkeeper.ui.shared.DeleteConfirmDialog
import com.example.billkeeper.ui.shared.EditBillDialog
import com.example.billkeeper.ui.shared.EditIncomeDialog
import com.example.billkeeper.viewmodel.LedgerViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BillKeeperApp(vm: LedgerViewModel) {
    val tabs = listOf("支出总览", "记录支出", "录入收入")
    val pagerState = rememberPagerState { tabs.size }
    val coroutineScope = rememberCoroutineScope()

    val totalExp by vm.totalExpense.collectAsStateWithLifecycle(initialValue = 0L)
    val totalInc by vm.totalIncome.collectAsStateWithLifecycle(initialValue = 0L)

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        vm.snackbarMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
        }
    }

    var billToDelete by remember { mutableStateOf<BillItem?>(null) }
    var incomeToDelete by remember { mutableStateOf<IncomeItem?>(null) }
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
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomSummaryItem("总收入", totalInc, Color(0xFF2E7D32))
                    BottomSummaryItem("总支出", totalExp, Color(0xFFC62828))
                    BottomSummaryItem(
                        "结余",
                        totalInc - totalExp,
                        if (totalInc - totalExp >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
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
                                fontWeight = if (pagerState.currentPage == idx) {
                                    FontWeight.Bold
                                } else {
                                    FontWeight.Normal
                                }
                            )
                        }
                    )
                }
            }

            HorizontalPager(
                beyondBoundsPageCount = 0,
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                when (page) {
                    0 -> ExpenseSummaryTab(vm)
                    1 -> ImportBillTab(
                        vm,
                        onEditBill = { billToEdit = it },
                        onDeleteBill = { billToDelete = it }
                    )
                    2 -> AddIncomeTab(
                        vm,
                        onEditIncome = { incomeToEdit = it },
                        onDeleteIncome = { incomeToDelete = it }
                    )
                }
            }
        }
    }

    billToDelete?.let { bill ->
        DeleteConfirmDialog(
            title = "${bill.category} ${formatCurrency(bill.amountCents)}",
            onConfirm = { vm.deleteBill(bill); billToDelete = null },
            onDismiss = { billToDelete = null }
        )
    }
    incomeToDelete?.let { income ->
        DeleteConfirmDialog(
            title = "${income.source} ${formatCurrency(income.amountCents)}",
            onConfirm = { vm.deleteIncome(income); incomeToDelete = null },
            onDismiss = { incomeToDelete = null }
        )
    }

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
