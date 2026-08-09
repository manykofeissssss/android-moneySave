package com.example.billkeeper.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.billkeeper.data.local.entity.BillItem
import com.example.billkeeper.data.local.entity.IncomeItem
import com.example.billkeeper.background.BackgroundPreferences
import com.example.billkeeper.background.AppearancePreferences
import com.example.billkeeper.background.AppearanceSettings
import com.example.billkeeper.ui.background.AppBackground
import com.example.billkeeper.ui.background.BackgroundSettingsDialog
import com.example.billkeeper.ui.screen.AddIncomeTab
import com.example.billkeeper.ui.screen.ExpenseSummaryTab
import com.example.billkeeper.ui.screen.ImportBillTab
import com.example.billkeeper.ui.screen.ReminderSettingsDialog
import com.example.billkeeper.ui.screen.RecurringEntryScreen
import com.example.billkeeper.ui.shared.BottomSummaryItem
import com.example.billkeeper.ui.shared.EditBillDialog
import com.example.billkeeper.ui.shared.EditIncomeDialog
import com.example.billkeeper.viewmodel.LedgerViewModel
import com.example.billkeeper.viewmodel.RecurringEntryViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BillKeeperApp(
    vm: LedgerViewModel,
    recurringVm: RecurringEntryViewModel,
    appearanceSettings: AppearanceSettings,
    appearancePreferences: AppearancePreferences,
    darkTheme: Boolean
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val backgroundPreferences = remember(context) { BackgroundPreferences(context) }
    var backgroundRevision by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val tabs = remember { listOf("支出总览", "记录支出", "录入收入") }
    val pagerState = rememberPagerState { tabs.size }
    val coroutineScope = rememberCoroutineScope()
    var showRecurringEntries by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = showRecurringEntries) {
        showRecurringEntries = false
    }

    val monthlyUiState by vm.monthlyUiState.collectAsStateWithLifecycle()
    val monthlyExpense = monthlyUiState.totalExpenseCents
    val monthlyIncome = monthlyUiState.totalIncomeCents

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        vm.snackbarEvents.collect { event ->
            val canUndo = event.deletedEntry != null
            val result = snackbarHostState.showSnackbar(
                message = event.message,
                actionLabel = if (canUndo) "撤销" else null,
                withDismissAction = canUndo,
                duration = if (canUndo) SnackbarDuration.Long else SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                event.deletedEntry?.let(vm::restoreDeletedEntry)
            }
        }
    }
    LaunchedEffect(Unit) {
        recurringVm.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    var billToEdit by remember { mutableStateOf<BillItem?>(null) }
    var incomeToEdit by remember { mutableStateOf<IncomeItem?>(null) }
    var showReminderSettings by remember { mutableStateOf(false) }
    var showBackgroundSettings by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        AppBackground(
            preferences = backgroundPreferences,
            settings = appearanceSettings,
            darkTheme = darkTheme,
            revision = backgroundRevision,
            modifier = Modifier.fillMaxSize()
        )
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (showRecurringEntries) "周期记账" else "小小账本",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (showRecurringEntries) {
                        IconButton(onClick = { showRecurringEntries = false }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回账本")
                        }
                    }
                },
                actions = {
                    if (!showRecurringEntries) {
                        IconButton(onClick = { showRecurringEntries = true }) {
                            Icon(Icons.Default.Repeat, contentDescription = "周期记账")
                        }
                        IconButton(onClick = { showBackgroundSettings = true }) {
                            Icon(Icons.Default.Wallpaper, contentDescription = "设置背景")
                        }
                        IconButton(onClick = { showReminderSettings = true }) {
                            Icon(Icons.Default.Notifications, contentDescription = "提醒设置")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            if (!showRecurringEntries) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BottomSummaryItem("本月收入", monthlyIncome, Color(0xFF2E7D32))
                        BottomSummaryItem("本月支出", monthlyExpense, Color(0xFFC62828))
                        BottomSummaryItem(
                            "本月结余",
                            monthlyIncome - monthlyExpense,
                            if (monthlyIncome - monthlyExpense >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (showRecurringEntries) {
            Box(modifier = Modifier.padding(padding)) {
                RecurringEntryScreen(recurringVm)
            }
        } else {
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
                    tabs.forEachIndexed { idx, title ->
                        Tab(
                            selected = pagerState.currentPage == idx,
                            onClick = { coroutineScope.launch { pagerState.scrollToPage(idx) } },
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
                    beyondBoundsPageCount = 1,
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
    }
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

    if (showReminderSettings) {
        ReminderSettingsDialog(onDismiss = { showReminderSettings = false })
    }
    if (showBackgroundSettings) {
        BackgroundSettingsDialog(
            preferences = backgroundPreferences,
            appearancePreferences = appearancePreferences,
            settings = appearanceSettings,
            darkTheme = darkTheme,
            onBackgroundChanged = { backgroundRevision++ },
            onDismiss = { showBackgroundSettings = false }
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
