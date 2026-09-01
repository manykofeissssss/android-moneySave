package com.example.billkeeper.ui.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.runtime.Composable
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
import com.xah.navigation.model.dest.Destination
import com.xah.navigation.util.LocalNavController
import com.xah.navigation.util.LocalNavDependencies

data class BillKeeperNavDependencies(
    val ledgerViewModel: LedgerViewModel,
    val recurringEntryViewModel: RecurringEntryViewModel,
    val dailyLedgerViewModel: DailyLedgerViewModel,
    val appearanceSettings: AppearanceSettings,
    val appearancePreferences: AppearancePreferences,
    val backgroundPreferences: BackgroundPreferences,
    val darkTheme: Boolean,
    val snackbarHostState: SnackbarHostState,
    val onBackgroundChanged: () -> Unit
)

@Composable
private fun billKeeperDependencies(): BillKeeperNavDependencies =
    LocalNavDependencies.current.get()

object HomeDestination : Destination() {
    override val key = "home"

    @Composable
    override fun Content() {
        val deps = billKeeperDependencies()
        val navController = LocalNavController.current

        HomeScreen(
            vm = deps.ledgerViewModel,
            snackbarHostState = deps.snackbarHostState,
            onOpenMoreFeatures = {
                navController.push(MoreFeaturesDestination)
            }
        )
    }
}

object MoreFeaturesDestination : Destination() {
    override val key = "more_features"

    @Composable
    override fun Content() {
        val deps = billKeeperDependencies()
        val navController = LocalNavController.current

        FeaturePageScaffold(
            title = "更多功能",
            onBack = { navController.pop() },
            snackbarHostState = deps.snackbarHostState
        ) {
            MoreFeaturesScreen(
                onOpenDailyLedger = { navController.push(DailyLedgerDestination) },
                onOpenRecurringEntries = { navController.push(RecurringEntriesDestination) },
                onOpenReminderSettings = { navController.push(ReminderSettingsDestination) },
                onOpenAppearanceSettings = { navController.push(AppearanceSettingsDestination) }
            )
        }
    }
}

object DailyLedgerDestination : Destination() {
    override val key = "daily_ledger"

    @Composable
    override fun Content() {
        val deps = billKeeperDependencies()
        val navController = LocalNavController.current
        FeaturePageScaffold(
            title = "单日账单查询",
            titleIcon = Icons.Default.CalendarMonth,
            sharedContainerKey = "daily_ledger",
            onBack = { navController.pop() },
            snackbarHostState = deps.snackbarHostState
        ) {
            DailyLedgerScreen(deps.dailyLedgerViewModel)
        }
    }
}

object RecurringEntriesDestination : Destination() {
    override val key = "recurring_entries"

    @Composable
    override fun Content() {
        val deps = billKeeperDependencies()
        val navController = LocalNavController.current
        FeaturePageScaffold(
            title = "周期记账",
            titleIcon = Icons.Default.Repeat,
            sharedContainerKey = "recurring_entries",
            onBack = { navController.pop() },
            snackbarHostState = deps.snackbarHostState
        ) {
            RecurringEntryScreen(deps.recurringEntryViewModel)
        }
    }
}

object ReminderSettingsDestination : Destination() {
    override val key = "reminder_settings"

    @Composable
    override fun Content() {
        val deps = billKeeperDependencies()
        val navController = LocalNavController.current
        FeaturePageScaffold(
            title = "记账提醒",
            titleIcon = Icons.Default.Notifications,
            sharedContainerKey = "reminder_settings",
            onBack = { navController.pop() },
            snackbarHostState = deps.snackbarHostState
        ) {
            ReminderSettingsScreen()
        }
    }
}

object AppearanceSettingsDestination : Destination() {
    override val key = "appearance_settings"

    @Composable
    override fun Content() {
        val deps = billKeeperDependencies()
        val navController = LocalNavController.current
        FeaturePageScaffold(
            title = "外观设置",
            titleIcon = Icons.Default.Palette,
            sharedContainerKey = "appearance_settings",
            onBack = { navController.pop() },
            snackbarHostState = deps.snackbarHostState
        ) {
            BackgroundSettingsScreen(
                preferences = deps.backgroundPreferences,
                appearancePreferences = deps.appearancePreferences,
                settings = deps.appearanceSettings,
                darkTheme = deps.darkTheme,
                onBackgroundChanged = deps.onBackgroundChanged
            )
        }
    }
}
