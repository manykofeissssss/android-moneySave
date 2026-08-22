package com.example.billkeeper.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.billkeeper.background.AppearancePreferences
import com.example.billkeeper.background.AppearanceSettings
import com.example.billkeeper.background.BackgroundPreferences
import com.example.billkeeper.ui.background.BackgroundSettingsScreen
import com.example.billkeeper.ui.screen.DailyLedgerScreen
import com.example.billkeeper.ui.screen.HomeScreen
import com.example.billkeeper.ui.screen.MoreFeaturesScreen
import com.example.billkeeper.ui.screen.RecurringEntryScreen
import com.example.billkeeper.ui.screen.ReminderSettingsScreen
import com.example.billkeeper.viewmodel.DailyLedgerViewModel
import com.example.billkeeper.viewmodel.LedgerViewModel
import com.example.billkeeper.viewmodel.RecurringEntryViewModel
import kotlinx.coroutines.launch

@Composable
fun BillKeeperNavigation(
    ledgerViewModel: LedgerViewModel,
    recurringEntryViewModel: RecurringEntryViewModel,
    dailyLedgerViewModel: DailyLedgerViewModel,
    appearanceSettings: AppearanceSettings,
    appearancePreferences: AppearancePreferences,
    backgroundPreferences: BackgroundPreferences,
    darkTheme: Boolean,
    snackbarHostState: SnackbarHostState,
    onBackgroundChanged: () -> Unit
) {
    val backStack = rememberNavBackStack(HomeDestination)
    val scope = rememberCoroutineScope()
    val navigateBack = { if (backStack.size > 1) backStack.removeLastOrNull() }

    NavDisplay(
        backStack = backStack,
        modifier = Modifier.fillMaxSize(),
        onBack = navigateBack,
        entryProvider = entryProvider {
            entry<HomeDestination> {
                HomeScreen(
                    vm = ledgerViewModel,
                    snackbarHostState = snackbarHostState,
                    onOpenMoreFeatures = { backStack.add(MoreFeaturesDestination) }
                )
            }
            entry<MoreFeaturesDestination> {
                FeaturePageScaffold(
                    title = "更多功能",
                    onBack = navigateBack,
                    snackbarHostState = snackbarHostState
                ) {
                    MoreFeaturesScreen(
                        onOpenDailyLedger = { backStack.add(DailyLedgerDestination) },
                        onOpenRecurringEntries = { backStack.add(RecurringEntriesDestination) },
                        onOpenReminderSettings = { backStack.add(ReminderSettingsDestination) },
                        onOpenAppearanceSettings = { backStack.add(AppearanceSettingsDestination) }
                    )
                }
            }
            entry<DailyLedgerDestination> {
                FeaturePageScaffold(
                    title = "单日账单查询",
                    onBack = navigateBack,
                    snackbarHostState = snackbarHostState
                ) {
                    DailyLedgerScreen(dailyLedgerViewModel)
                }
            }
            entry<RecurringEntriesDestination> {
                FeaturePageScaffold(
                    title = "周期记账",
                    onBack = navigateBack,
                    snackbarHostState = snackbarHostState
                ) {
                    RecurringEntryScreen(recurringEntryViewModel)
                }
            }
            entry<ReminderSettingsDestination> {
                FeaturePageScaffold(
                    title = "记账提醒",
                    onBack = navigateBack,
                    snackbarHostState = snackbarHostState
                ) {
                    ReminderSettingsScreen()
                }
            }
            entry<AppearanceSettingsDestination> {
                FeaturePageScaffold(
                    title = "外观设置",
                    onBack = navigateBack,
                    snackbarHostState = snackbarHostState
                ) {
                    BackgroundSettingsScreen(
                        preferences = backgroundPreferences,
                        appearancePreferences = appearancePreferences,
                        settings = appearanceSettings,
                        darkTheme = darkTheme,
                        onBackgroundChanged = {
                            onBackgroundChanged()
                            scope.launch { snackbarHostState.showSnackbar("外观设置已应用") }
                        }
                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeaturePageScaffold(
    title: String,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    content: @Composable () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.88f),
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回更多功能")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            content()
        }
    }
}
