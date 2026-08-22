package com.example.billkeeper.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.billkeeper.data.local.entity.BillItem
import com.example.billkeeper.data.local.entity.IncomeItem
import com.example.billkeeper.ui.shared.BottomSummaryItem
import com.example.billkeeper.ui.shared.EditBillDialog
import com.example.billkeeper.ui.shared.EditIncomeDialog
import com.example.billkeeper.viewmodel.LedgerViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    vm: LedgerViewModel,
    snackbarHostState: SnackbarHostState,
    onOpenMoreFeatures: () -> Unit
) {
    val tabs = remember { listOf("支出总览", "记录支出", "录入收入") }
    val pagerState = rememberPagerState { tabs.size }
    val coroutineScope = rememberCoroutineScope()
    val monthlyUiState by vm.monthlyUiState.collectAsStateWithLifecycle()
    var billToEdit by remember { mutableStateOf<BillItem?>(null) }
    var incomeToEdit by remember { mutableStateOf<IncomeItem?>(null) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("小小账本", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onOpenMoreFeatures) {
                        Icon(Icons.Default.MoreVert, contentDescription = "更多功能")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val balance = monthlyUiState.totalIncomeCents - monthlyUiState.totalExpenseCents
                    BottomSummaryItem("本月收入", monthlyUiState.totalIncomeCents, Color(0xFF2E7D32))
                    BottomSummaryItem("本月支出", monthlyUiState.totalExpenseCents, Color(0xFFC62828))
                    BottomSummaryItem(
                        "本月结余",
                        balance,
                        if (balance >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            MonthSelectorBar(
                monthLabel = monthlyUiState.monthLabel,
                onPreviousMonth = vm::goToPreviousMonth,
                onNextMonth = vm::goToNextMonth,
                onCurrentMonth = vm::jumpToCurrentMonth
            )
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { coroutineScope.launch { pagerState.scrollToPage(index) } },
                        text = {
                            Text(
                                title,
                                fontWeight = if (pagerState.currentPage == index) {
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
                beyondViewportPageCount = 1,
                state = pagerState,
                key = { page -> page },
                userScrollEnabled = true,
                modifier = Modifier.weight(1f)
            ) { page ->
                when (page) {
                    0 -> ExpenseSummaryTab(vm)
                    1 -> ImportBillTab(
                        vm,
                        onEditBill = { billToEdit = it },
                        onDeleteBill = vm::deleteBill
                    )
                    2 -> AddIncomeTab(
                        vm,
                        onEditIncome = { incomeToEdit = it },
                        onDeleteIncome = vm::deleteIncome
                    )
                }
            }
        }
    }

    billToEdit?.let { bill ->
        EditBillDialog(
            bill = bill,
            onSave = {
                vm.updateBill(it)
                billToEdit = null
            },
            onDismiss = { billToEdit = null }
        )
    }
    incomeToEdit?.let { income ->
        EditIncomeDialog(
            income = income,
            onSave = {
                vm.updateIncome(it)
                incomeToEdit = null
            },
            onDismiss = { incomeToEdit = null }
        )
    }
}

@Composable
private fun MonthSelectorBar(
    monthLabel: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onCurrentMonth: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        tonalElevation = 1.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            IconButton(
                onClick = onPreviousMonth,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "上月")
            }
            Text(
                text = monthLabel,
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Row(modifier = Modifier.align(Alignment.CenterEnd)) {
                IconButton(onClick = onCurrentMonth) {
                    Icon(Icons.Default.Today, contentDescription = "回到本月")
                }
                IconButton(onClick = onNextMonth) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "下月")
                }
            }
        }
    }
}
