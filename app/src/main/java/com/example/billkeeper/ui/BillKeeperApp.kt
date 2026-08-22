package com.example.billkeeper.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.billkeeper.background.AppearancePreferences
import com.example.billkeeper.background.AppearanceSettings
import com.example.billkeeper.background.BackgroundPreferences
import com.example.billkeeper.ui.background.AppBackground
import com.example.billkeeper.ui.navigation.BillKeeperNavigation
import com.example.billkeeper.viewmodel.DailyLedgerViewModel
import com.example.billkeeper.viewmodel.LedgerViewModel
import com.example.billkeeper.viewmodel.RecurringEntryViewModel

@Composable
fun BillKeeperApp(
    vm: LedgerViewModel,
    recurringVm: RecurringEntryViewModel,
    dailyLedgerVm: DailyLedgerViewModel,
    appearanceSettings: AppearanceSettings,
    appearancePreferences: AppearancePreferences,
    darkTheme: Boolean
) {
    val context = LocalContext.current
    val backgroundPreferences = remember(context) { BackgroundPreferences(context) }
    val snackbarHostState = remember { SnackbarHostState() }
    var backgroundRevision by remember { mutableIntStateOf(0) }

    LaunchedEffect(vm) {
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
    LaunchedEffect(recurringVm) {
        recurringVm.messages.collect(snackbarHostState::showSnackbar)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AppBackground(
            preferences = backgroundPreferences,
            settings = appearanceSettings,
            darkTheme = darkTheme,
            revision = backgroundRevision,
            modifier = Modifier.fillMaxSize()
        )
        BillKeeperNavigation(
            ledgerViewModel = vm,
            recurringEntryViewModel = recurringVm,
            dailyLedgerViewModel = dailyLedgerVm,
            appearanceSettings = appearanceSettings,
            appearancePreferences = appearancePreferences,
            backgroundPreferences = backgroundPreferences,
            darkTheme = darkTheme,
            snackbarHostState = snackbarHostState,
            onBackgroundChanged = { backgroundRevision++ }
        )
    }
}
